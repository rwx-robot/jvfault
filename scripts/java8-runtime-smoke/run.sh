#!/usr/bin/env bash
#
# jvfault —— 在真正的 Java 8 JVM 上运行框架冒烟测试
#
# 为什么需要它：
#   框架宣称「Java 8 基线」，但日常测试全在 JDK 21 上跑（靠 --release 8 做编译期检查）。
#   编译期检查只能保证 API 兼容，无法证明字节码能被 Java 8 JVM 真正加载执行。
#   本脚本补上这一环：拉一个真 Java 8 JRE，把框架 jar 丢进去加载。
#
# 用法：
#   ./scripts/java8-runtime-smoke/run.sh
#
# 环境变量（可选）：
#   JAVA8_HOME  已装好的 Java 8 JRE/JDK 路径；不设则自动下载 Temurin 8 到 ~/.cache
#   JDK21_HOME  用于编译冒烟程序的 JDK；不设则猜 mac 默认路径
#
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
WORK="${ROOT}/build/java8-smoke"
JARS="${WORK}/jars"
CLASSES="${WORK}/classes"

# macOS 默认 JDK 21 路径（Linux 请自行设置 JDK21_HOME）
MAC_JDK21="/Library/Java/JavaVirtualMachines/jdk-21.0.11.jdk/Contents/Home"
JDK21_HOME="${JDK21_HOME:-${MAC_JDK21}}"

if [ ! -x "${JDK21_HOME}/bin/javac" ]; then
  echo "错误：找不到 JDK 21 的 javac：${JDK21_HOME}/bin/javac" >&2
  echo "请设置 JDK21_HOME，例如：JDK21_HOME=/path/to/jdk-21 ./run.sh" >&2
  exit 2
fi

# ---------- 1. 准备 Java 8 JRE ----------
if [ -z "${JAVA8_HOME:-}" ]; then
  JAVA8_HOME="${HOME}/.cache/jvfault-java8"
  if [ ! -x "${JAVA8_HOME}/bin/java" ]; then
    echo "==> 未设置 JAVA8_HOME，下载 Temurin 8 JRE 到 ${JAVA8_HOME}"
    mkdir -p "${JAVA8_HOME}"
    OS="$(uname -s)"; ARCH="$(uname -m)"
    case "${OS}" in
      Darwin) API_OS="mac" ;;
      Linux)  API_OS="linux" ;;
      *) echo "不支持的操作系统：${OS}" >&2; exit 2 ;;
    esac
    case "${ARCH}" in
      arm64|aarch64) API_ARCH="aarch64" ;;
      x86_64|amd64)  API_ARCH="x64" ;;
      *) echo "不支持的架构：${ARCH}" >&2; exit 2 ;;
    esac
    URL="https://api.adoptium.net/v3/binary/latest/8/ga/${API_OS}/${API_ARCH}/jre/hotspot/normal/eclipse"
    TMP="$(mktemp -d)"
    echo "    URL: ${URL}"
    curl -fsSL --retry 3 -o "${TMP}/jre8.tar.gz" "${URL}"
    tar -xzf "${TMP}/jre8.tar.gz" -C "${TMP}"
    # macOS 的产物带 Contents/Home，Linux 直接是顶层
    SRC="${TMP}/$(ls "${TMP}" | grep -E '^jdk8u' | head -1)"
    if [ -d "${SRC}/Contents/Home" ]; then SRC="${SRC}/Contents/Home"; fi
    cp -R "${SRC}/." "${JAVA8_HOME}/"
    rm -rf "${TMP}"
  fi
fi

if [ ! -x "${JAVA8_HOME}/bin/java" ]; then
  echo "错误：找不到 Java 8 运行时：${JAVA8_HOME}/bin/java" >&2; exit 2
fi
echo "==> Java 8 运行时：$("${JAVA8_HOME}/bin/java" -version 2>&1 | head -1)"

# ---------- 2. 收集 jar ----------
echo "==> 收集框架 jar"
rm -rf "${JARS}" "${CLASSES}"; mkdir -p "${JARS}" "${CLASSES}"
FOUND=0
while IFS= read -r jar; do
  cp "${jar}" "${JARS}/"; FOUND=$((FOUND+1))
done < <(find "${ROOT}" -path '*/build/libs/jvfault-*.jar' \
          ! -name '*sources*' ! -name '*javadoc*' 2>/dev/null)

if [ "${FOUND}" -eq 0 ]; then
  echo "没有找到任何 jar，先执行一次：./gradlew jar" >&2
  exit 2
fi
echo "    共 ${FOUND} 个 jar"

# slf4j-api 是 JvfaultApplication 的硬依赖（不带上会 NoClassDefFoundError 假失败）
# 从 Gradle 缓存里翻出来，避免联网
SLF4J="$(find "${HOME}/.gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-api" \
         -name 'slf4j-api-*.jar' ! -name '*sources*' 2>/dev/null | head -1)"
if [ -n "${SLF4J}" ]; then
  cp "${SLF4J}" "${JARS}/"
  echo "    附带 slf4j-api: $(basename "${SLF4J}")"
else
  echo "    警告：未在 Gradle 缓存中找到 slf4j-api，core 可能加载失败" >&2
fi

# ---------- 3. 编译冒烟程序（生成 Java 8 字节码）----------
echo "==> 编译冒烟程序（--release 8）"
CP="$(find "${JARS}" -name '*.jar' | tr '\n' ':')"
"${JDK21_HOME}/bin/javac" --release 8 -nowarn \
  -cp "${CP}" -d "${CLASSES}" "${HERE}/Java8RuntimeSmoke.java"

# ---------- 4. 在真 Java 8 上运行 ----------
echo "==> 在 Java 8 上运行"
echo ""
"${JAVA8_HOME}/bin/java" -cp "${CLASSES}:${CP}" Java8RuntimeSmoke "${JARS}"
