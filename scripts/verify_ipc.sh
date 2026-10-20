#!/usr/bin/env bash
#
# 验证车辆域跨进程链路：杀掉 :vehicle 服务进程，主进程应保持不变，
# 并在 3 秒重连后重新 bindService、注册监听、收到状态快照。
#
# 用法：
#   ./scripts/verify_ipc.sh                       # 自动选择唯一在线设备
#   ./scripts/verify_ipc.sh -s emulator-5554     # 指定设备
#   ./scripts/verify_ipc.sh -s emulator-5554 --mode root
#   ./scripts/verify_ipc.sh --no-clear           # 不清空 logcat
#
# 两种杀进程方式：
#   crash（默认）：adb shell am crash --user <uid> <vehicle-pid>，无需 root
#   root        ：adb root 后 kill -9 <vehicle-pid>，需要 userdebug/eng 镜像
#
# 注意：不要用 am force-stop、am kill、killall 或 IDE 的 Stop/Terminate，
# 它们会杀掉整个应用（主进程 + :vehicle），无法验证“只死服务进程”的重连。

set -euo pipefail

PACKAGE="com.oem.car.app"
MAIN_COMPONENT="${PACKAGE}/.MainActivity"
MAIN_PROC="${PACKAGE}"
VEHICLE_PROC="${PACKAGE}:vehicle"

ADB_BIN="${ADB:-adb}"
SERIAL=""
MODE="crash"
CLEAR_LOG=1
TIMEOUT=20

usage() {
    cat <<'EOF'
验证车辆域跨进程链路：杀掉 :vehicle 服务进程，主进程应保持不变，
并在 3 秒重连后重新 bindService、注册监听、收到状态快照。

用法：
  ./scripts/verify_ipc.sh                       # 自动选择唯一在线设备
  ./scripts/verify_ipc.sh -s emulator-5554      # 指定设备
  ./scripts/verify_ipc.sh -s emulator-5554 --mode root
  ./scripts/verify_ipc.sh --no-clear            # 不清空 logcat

两种杀进程方式：
  crash（默认）：adb shell am crash --user <uid> <vehicle-pid>，无需 root
  root        ：adb root 后 kill -9 <vehicle-pid>，需要 userdebug/eng 镜像

注意：不要用 am force-stop、am kill、killall 或 IDE 的 Stop/Terminate，
它们会杀掉整个应用（主进程 + :vehicle），无法验证“只死服务进程”的重连。
EOF
}

die() {
    echo "ERROR: $*" >&2
    exit 2
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        -s|--serial)
            [[ $# -ge 2 ]] || die "$1 需要设备序列号"
            SERIAL="$2"
            shift 2
            ;;
        --mode)
            [[ $# -ge 2 ]] || die "$1 需要 crash 或 root"
            MODE="$2"
            shift 2
            ;;
        --timeout)
            [[ $# -ge 2 ]] || die "$1 需要秒数"
            TIMEOUT="$2"
            shift 2
            ;;
        --no-clear)
            CLEAR_LOG=0
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "未知参数：$1" >&2
            usage >&2
            exit 2
            ;;
    esac
done

[[ "$MODE" == "crash" || "$MODE" == "root" ]] || die "--mode 只支持 crash 或 root"
[[ "$TIMEOUT" =~ ^[0-9]+$ ]] && (( TIMEOUT > 0 )) || die "--timeout 必须是正整数"
command -v "$ADB_BIN" >/dev/null 2>&1 || die "找不到 adb，可设置 ADB=/path/to/adb"

if [[ -z "$SERIAL" ]]; then
    devices=()
    while IFS=$'\t' read -r serial state; do
        [[ "$state" == "device" ]] && devices+=("$serial")
    done < <("$ADB_BIN" devices | tail -n +2 | tr -d '\r')

    if (( ${#devices[@]} == 0 )); then
        die "没有在线设备，请先启动模拟器或连接真机（adb devices）"
    fi
    if (( ${#devices[@]} > 1 )); then
        printf '在线设备不止一个，请用 -s 指定：\n' >&2
        printf '  %s\n' "${devices[@]}" >&2
        exit 2
    fi
    SERIAL="${devices[0]}"
fi

ADB=("$ADB_BIN" -s "$SERIAL")

proc_pid() {
    local name="$1"
    "${ADB[@]}" shell pidof "$name" 2>/dev/null \
        | tr -d '\r' \
        | awk 'NF {print $1; exit}' \
        || true
}

wait_for_pid() {
    local name="$1"
    local wanted="$2"
    local deadline=$((SECONDS + TIMEOUT))
    local pid=""
    while (( SECONDS < deadline )); do
        pid="$(proc_pid "$name")"
        if [[ -n "$pid" && "$pid" != "$wanted" ]]; then
            printf '%s' "$pid"
            return 0
        fi
        sleep 0.5
    done
    return 1
}

echo "== 设备与进程检查 =="
echo "serial         : $SERIAL"

USER_ID="$("${ADB[@]}" shell am get-current-user 2>/dev/null | tr -d '\r' | awk 'NF {print $1; exit}' || true)"
[[ "$USER_ID" =~ ^[0-9]+$ ]] || USER_ID=0
echo "current user   : $USER_ID"

if [[ -z "$(proc_pid "$MAIN_PROC")" ]]; then
    echo "主进程未运行，启动 $MAIN_COMPONENT ..."
    "${ADB[@]}" shell am start --user "$USER_ID" -n "$MAIN_COMPONENT" >/dev/null
    MAIN_PID="$(wait_for_pid "$MAIN_PROC" "")" || die "主进程启动超时"
else
    MAIN_PID="$(proc_pid "$MAIN_PROC")"
fi
echo "main pid       : $MAIN_PID"

OLD_VEHICLE_PID="$(proc_pid "$VEHICLE_PROC")"
if [[ -z "$OLD_VEHICLE_PID" ]]; then
    echo "等待 bindService 拉起 :vehicle 进程 ..."
    OLD_VEHICLE_PID="$(wait_for_pid "$VEHICLE_PROC" "")" || die ":vehicle 进程启动超时"
fi
echo "vehicle pid    : $OLD_VEHICLE_PID"

[[ "$CLEAR_LOG" -eq 1 ]] && "${ADB[@]}" logcat -c >/dev/null 2>&1 || true

TOKEN="VERIFY_$(date +%s)_$$"
"${ADB[@]}" shell log -t IPCVerify "START $TOKEN" >/dev/null 2>&1 || true

echo
echo "== 只杀 :vehicle 进程（mode=$MODE）=="
case "$MODE" in
    crash)
        echo "adb shell am crash --user $USER_ID $OLD_VEHICLE_PID"
        crash_output="$("${ADB[@]}" shell am crash --user "$USER_ID" "$OLD_VEHICLE_PID" 2>&1 | tr -d '\r' || true)"
        [[ -n "$crash_output" ]] && echo "  $crash_output"
        ;;
    root)
        root_output="$("${ADB[@]}" root 2>&1 | tr -d '\r' || true)"
        [[ -n "$root_output" ]] && echo "  $root_output"
        if [[ "$root_output" == *"cannot run as root"* || "$root_output" == *"production"* ]]; then
            die "该镜像不允许 adb root，请改用默认 --mode crash"
        fi
        "${ADB[@]}" wait-for-device
        echo "adb shell kill -9 $OLD_VEHICLE_PID"
        "${ADB[@]}" shell kill -9 "$OLD_VEHICLE_PID" >/dev/null
        ;;
esac

echo
echo "== 等待服务进程重建与客户端重连（最长 ${TIMEOUT}s）=="
NEW_VEHICLE_PID="$(wait_for_pid "$VEHICLE_PROC" "$OLD_VEHICLE_PID")" \
    || die ":vehicle 未在 ${TIMEOUT}s 内重建；请查看 adb logcat -s VehicleServiceConn VehicleCoreService CarCoreDaemon"

LOG_DIR="$(mktemp -d "${TMPDIR:-/tmp}/verify-ipc.XXXXXX")"
LOG_RAW="$LOG_DIR/raw.log"
LOG_AFTER="$LOG_DIR/after-marker.log"
RECONNECT_PATTERN="registerListener registered=true callerPid=${MAIN_PID} myPid=${NEW_VEHICLE_PID}"
deadline=$((SECONDS + TIMEOUT))
while (( SECONDS < deadline )); do
    "${ADB[@]}" logcat -d -v threadtime \
        -s VehicleServiceConn:V VehicleCoreService:V CarCoreDaemon:V AndroidRuntime:E IPCVerify:V \
        > "$LOG_RAW" 2>/dev/null || true

    if grep -Fq "$TOKEN" "$LOG_RAW"; then
        awk -v token="$TOKEN" 'index($0, token) {found=1; next} found {print}' \
            "$LOG_RAW" > "$LOG_AFTER"
    else
        cp "$LOG_RAW" "$LOG_AFTER"
    fi

    if grep -Fq "$RECONNECT_PATTERN" "$LOG_AFTER" \
        && grep -Fq "binder=android.os.BinderProxy" "$LOG_AFTER"; then
        break
    fi
    sleep 0.5
done

echo
echo "== 断言 =="
FAILURES=0

check() {
    local label="$1"
    local ok="$2"
    local detail="${3:-}"
    if [[ "$ok" -eq 1 ]]; then
        echo "PASS  $label${detail:+  ($detail)}"
    else
        echo "FAIL  $label${detail:+  ($detail)}"
        FAILURES=$((FAILURES + 1))
    fi
}

CURRENT_MAIN_PID="$(proc_pid "$MAIN_PROC")"
[[ "$CURRENT_MAIN_PID" == "$MAIN_PID" ]] && main_ok=1 || main_ok=0
check "主进程未被杀，PID 保持不变" "$main_ok" "$MAIN_PID -> ${CURRENT_MAIN_PID:-<none>}"

[[ -n "$NEW_VEHICLE_PID" && "$NEW_VEHICLE_PID" != "$OLD_VEHICLE_PID" ]] && vehicle_ok=1 || vehicle_ok=0
check ":vehicle 已重建且 PID 变化" "$vehicle_ok" "$OLD_VEHICLE_PID -> $NEW_VEHICLE_PID"

grep -Fq "scheduleReconnect in 3000ms" "$LOG_AFTER" && reconnect_ok=1 || reconnect_ok=0
check "客户端触发 3 秒重连" "$reconnect_ok"

grep -Fq "binder=android.os.BinderProxy" "$LOG_AFTER" && proxy_ok=1 || proxy_ok=0
check "服务端 Binder 是 BinderProxy（非本进程对象）" "$proxy_ok"

grep -Fq "localInterface=null" "$LOG_AFTER" && local_ok=1 || local_ok=0
check "queryLocalInterface 返回 null（证明跨进程）" "$local_ok"

grep -Fq "$RECONNECT_PATTERN" "$LOG_AFTER" && register_ok=1 || register_ok=0
check "新服务进程收到 registerListener 且 callerPid 为主进程" "$register_ok" \
    "callerPid=$MAIN_PID myPid=$NEW_VEHICLE_PID"

grep -Fq "push snapshot speed=" "$LOG_AFTER" && snapshot_ok=1 || snapshot_ok=0
check "服务端注册后回推状态快照" "$snapshot_ok"

grep -Fq "snapshot onSpeedChanged" "$LOG_AFTER" && callback_ok=1 || callback_ok=0
check "客户端收到 oneway 状态回调" "$callback_ok"

echo
echo "== 关键日志 =="
grep -E "onCreate pid=|registerListener|push snapshot|scheduleReconnect|Connected pid=|binder died|Service disconnected|snapshot onSpeedChanged" \
    "$LOG_AFTER" | tail -n 30 || true

echo
if (( FAILURES == 0 )); then
    echo "RESULT: PASS"
    echo "完整日志：$LOG_AFTER"
    exit 0
else
    echo "RESULT: FAIL ($FAILURES 项未通过)"
    echo "完整日志：$LOG_AFTER"
    tail -n 40 "$LOG_AFTER" || true
    exit 1
fi
