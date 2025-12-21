#!/usr/bin/env bash
#
# 校验 README 宣称的测试总数 == 实际运行产出的测试总数。
#
# 背景（为什么需要它）：
#   测试总数是 jvfault README 里**唯一无法自我校验**的宣称 —— 它由 Gradle 产出，
#   JUnit 测试本身看不到自己的汇总数。`ReadmeFactsConsistencyTest` 只能比对
#   「README ↔ 常量」，对不上真实产出。
#   v1.0.9 就栽在这：新增了 7 个测试却没重算，README 静默写着 274（真实 281）。
#
# 本脚本把真实总数与 README badge 锁死，挂进 CI 后，任何
# 「改了测试数却没同步文档」都会在 CI 上直接红。
#
# 用法（在 jvfault 仓库根目录，且已跑过 ./gradlew test）：
#   ./scripts/verify-readme-test-count.sh
#
set -eu

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

README="${ROOT}/README.md"

if [ ! -f "${README}" ]; then
    echo "ERROR: 找不到 README.md：${README}" >&2
    exit 1
fi

# 1) README badge 里宣称的数字。badge URL 中空格被编码为 %20，两种形态都要认。
#
#    必须用 sed 抓 `tests-` 后面紧跟的那一段数字。
#    不能再用 `grep -oE '[0-9]+'` 二次提取 —— '%20' 里也有数字，会把 20 一并抓进来
#    （实测得到 "281\n20"），进而让下面的整数比较报错；而该比较在 if 里失败会被
#    当成 false 走到「通过」分支 —— **假通过，恰好掩盖漂移**。
DECLARED="$(grep -oE 'tests-[0-9]+(%20|[[:space:]]+)passing' "${README}" | head -n 1 | sed -E 's/^tests-([0-9]+).*$/\1/' || true)"

# 防御：宣称值必须是纯数字，否则下面的整数比较会重蹈上面「假通过」的覆辙。
case "${DECLARED}" in
    ''|*[!0-9]*)
        echo "ERROR: 从 README 解析出的测试数不是纯数字：'${DECLARED}'" >&2
        exit 1
        ;;
esac

# 2) 汇总所有模块的 JUnit XML 报告，得到实际测试总数。
#
#    刻意用 shell glob 枚举候选目录，而**不用** `find . -path ...`：
#      - `find .` 会把 .git 与每个 build/ 子目录全都遍历一遍，
#        实测慢到几分钟，还会派生成千上万个进程把 shell 拖死（SIGTERM）。
#      - 报告目录是固定深度（<module>/build/test-results/test），
#        glob 直接命中，秒级完成。
#
#    统计的是 tests 属性（= 发现的用例数，含 skipped），
#    因此在「有无 broker」的不同环境下都稳定 —— 这是能挂 CI 断言的前提。
REPORT_DIRS=""
for d in */build/test-results/test examples/*/build/test-results/test; do
    if [ -d "$d" ]; then
        REPORT_DIRS="${REPORT_DIRS} $d"
    fi
done

ACTUAL=0
if [ -n "${REPORT_DIRS}" ]; then
    ACTUAL="$(find ${REPORT_DIRS} -maxdepth 1 -name '*.xml' -type f \
        -exec grep -hoE 'tests="[0-9]+"' {} + 2>/dev/null \
        | grep -oE '[0-9]+' \
        | awk '{ s += $1 } END { print s + 0 }')"
fi

if [ "${ACTUAL}" -eq 0 ]; then
    echo "ERROR: 没找到任何测试报告（<module>/build/test-results/test/*.xml）。" >&2
    echo "       请先跑 ./gradlew test 再执行本脚本。" >&2
    echo "       注意：若 gradle 判定 UP-TO-DATE 而未真正重跑，报告可能是旧的。" >&2
    exit 1
fi

echo "README badge 宣称测试数 : ${DECLARED}"
echo "JUnit 报告实测测试总数 : ${ACTUAL}"

if [ "${DECLARED}" -ne "${ACTUAL}" ]; then
    echo "" >&2
    # 差值单独一行算（不内联到 echo 里）：bash 3.2 在双引号 + 多字节字符 + set -u
    # 的组合下解析 ${...}／算术时会偶发误判。
    DELTA=$(expr "${ACTUAL}" - "${DECLARED}")
    echo "✗ 漂移：README 写的是 ${DECLARED}，实际是 ${ACTUAL}（差 ${DELTA}）" >&2
    echo "" >&2
    echo "修法 —— 把下面三处统一改成 ${ACTUAL}：" >&2
    echo "  1) README.md  Tests badge      -> tests-${ACTUAL} passing" >&2
    echo "  2) README.md  「维度」表「测试」行 + 快速开始段落的「共 N 个测试」" >&2
    echo "  3) tests/src/test/java/com/jvfault/tests/ReadmeFactsConsistencyTest.java" >&2
    echo "     -> EXPECTED_TEST_COUNT 常量" >&2
    exit 1
fi

echo "✓ 一致（${ACTUAL} 个测试）"
