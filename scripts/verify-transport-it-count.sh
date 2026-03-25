#!/usr/bin/env bash
#
# 校验 README 宣称的「传输集成测试」数量 == 源码里真正标记为集成测试的用例数。
#
# 背景（为什么需要它）：
#   README 写「18 个传输集成测试在 CI 上真实执行」，这个数**之前没有机检**，
#   只会随开发者增删测试而静默漂移。它和 test 总数不同 —— test 总数能靠 JUnit
#   报告对拍，但「集成测试」没有统一 tag，只能靠命名 / 是否起真实 broker 来判断。
#
# 判定规则（静态源码分析，与运行环境无关，本地/CI 结果一致）：
#   一个 transport 测试类算「集成测试」当且仅当满足其一：
#     1) 文件名以 `IntegrationTest.java` 结尾；或
#     2) 类里既有 `@BeforeAll`，又引用了真实容器/服务
#        （`DockerContainer` / `GenericContainer` / `Testcontainers`）。
#   —— 这能排除 KafkaTransportTest 这类「引用了 DockerContainer 但只在 @BeforeAll
#      之外用、或根本没起 broker」的单元级测试，也排除纯 mock 的 *TransportTest。
#   计数单位：类里 `@Test` 注解的方法数之和。
#
# 用法（在 jvfault 仓库根目录）：
#   ./scripts/verify-transport-it-count.sh [EXPECTED]
#   EXPECTED 缺省为 18（与 README 当前宣称一致）；传参则按传入值断言。
#
set -eu

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

README="${ROOT}/README.md"
EXPECTED="${1:-18}"

# 1) 扫描 7 个 transport 模块的 src/test，按规则统计集成测试用例数。
TOTAL=0
DETAIL=""

for f in $(find transport-tcp transport-grpc transport-kafka transport-redis transport-nats transport-rmq transport-mqtt \
              -path '*/src/test/*.java' 2>/dev/null | sort); do
    base="$(basename "$f")"
    [ "$base" = "DockerContainer.java" ] && continue

    is_it=0
    case "$base" in
        *IntegrationTest.java) is_it=1 ;;
    esac
    if [ "$is_it" -eq 0 ]; then
        if grep -q '@BeforeAll' "$f" && grep -qE 'DockerContainer|GenericContainer|Testcontainers' "$f"; then
            is_it=1
        fi
    fi

    if [ "$is_it" -eq 1 ]; then
        cnt="$(grep -c '@Test' "$f")"
        TOTAL=$((TOTAL + cnt))
        DETAIL="${DETAIL}  + ${base}: ${cnt}  (@Test)\n"
    fi
done

printf "传输集成测试类 / 用例明细：\n${DETAIL}"
printf "集成测试用例总数 : %s\n" "${TOTAL}"
printf "README 宣称数量  : %s\n" "${EXPECTED}"

if [ "${TOTAL}" -ne "${EXPECTED}" ]; then
    echo "" >&2
    echo "✗ 漂移：源码实际 ${TOTAL} 个传输集成测试，与宣称 ${EXPECTED} 不符。" >&2
    echo "  若确有增删，请同步两处：" >&2
    echo "    1) README.md 第 26 / 144 行的「18 个传输集成测试」表述" >&2
    echo "    2) 本脚本的 EXPECTED 默认值（或 CI 调用处的传参）" >&2
    exit 1
fi

echo "✓ 一致（${TOTAL} 个传输集成测试）"
