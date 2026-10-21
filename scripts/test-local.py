#!/usr/bin/env python3
"""Prepare this Mac's existing isolated test environment, then run the two suites.

Usage: python3 scripts/test-local.py [--check-only] [--verify]
No package installation, database provisioning, or application-data migration.
"""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import time


ROOT = Path(__file__).resolve().parent.parent
CONTAINER = "fitnutri-integration-20261016"


class CheckError(Exception):
    pass


def require(condition, message):
    if not condition:
        raise CheckError(message)


def capture(args, env, timeout=30):
    result = subprocess.run(args, cwd=ROOT, env=env, text=True,
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                            timeout=timeout)
    # Do not echo captured diagnostics: container inspection contains credentials.
    require(result.returncode == 0,
            "命令检查失败：{}。请检查对应服务或安装。".format(args[0]))
    return result.stdout.strip()


def announce(message):
    print("\n" + message, flush=True)


def prepare_runtimes(env):
    # JAVA_HOME is selected for this child environment only, never the user's shell.
    java_homes = [env.get("JAVA_HOME", ""),
                  "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home",
                  "/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"]
    for home in java_homes:
        if not home:
            continue
        java = Path(home) / "bin/java"
        javac = Path(home) / "bin/javac"
        if java.is_file() and javac.is_file():
            result = subprocess.run([str(java), "-version"], capture_output=True,
                                    text=True, timeout=10)
            if result.returncode == 0 and re.search(r'version "21\.', result.stderr + result.stdout):
                env["JAVA_HOME"] = home
                env["PATH"] = str(java.parent) + os.pathsep + env.get("PATH", "")
                break
    else:
        raise CheckError("未找到 JDK 21。安装或设置 JAVA_HOME 后重试。")

    node = shutil.which("node", path=env["PATH"])
    if not node or not capture([node, "--version"], env).startswith("v24."):
        nvm = Path(env.get("NVM_DIR", str(Path.home() / ".nvm")))
        candidates = sorted((nvm / "versions/node").glob("v24.*/bin/node"),
                            key=lambda p: tuple(map(int, p.parent.parent.name[1:].split("."))),
                            reverse=True)
        require(candidates, "未找到 Node 24。请安装 Node 24 后重试。")
        env["PATH"] = str(candidates[0].parent) + os.pathsep + env["PATH"]
    require(shutil.which("npm", path=env["PATH"]), "npm 不在 PATH 中。")
    require((ROOT / "frontend/node_modules").is_dir(),
            "缺少前端依赖。先运行 npm --prefix frontend ci。")
    capture(["npm", "--prefix", "frontend", "ls", "--all", "--parseable"], env)
    require((ROOT / "backend/mvnw").is_file() and
            os.access(ROOT / "backend/mvnw", os.X_OK), "backend/mvnw 缺失或不可执行。")
    version = capture([str(ROOT / "backend/mvnw"), "-f", "backend/pom.xml", "--version"], env)
    require("Java version: 21." in version, "Maven 没有使用 Java 21。")
    print("Node 24、npm、前端依赖、JDK 21、Maven Wrapper 检查通过。", flush=True)


def prepare_docker(env):
    require(shutil.which("docker", path=env["PATH"]), "未找到 Docker CLI。")

    def daemon_ready():
        try:
            result = subprocess.run(["docker", "info", "--format", "{{.ServerVersion}}"],
                                    env=env, capture_output=True, timeout=5)
            return result.returncode == 0
        except subprocess.TimeoutExpired:
            return False

    if not daemon_ready():
        require(sys.platform == "darwin", "Docker 服务未启动，请启动后重试。")
        announce("启动 Docker Desktop，最多等待 90 秒……")
        capture(["open", "-a", "Docker"], env)
        deadline = time.monotonic() + 90
        while not daemon_ready():
            require(time.monotonic() < deadline, "Docker 未在等待时间内就绪，请检查 Docker Desktop。")
            time.sleep(2)

    inspected = json.loads(capture(["docker", "inspect", CONTAINER], env))[0]
    require(not inspected["State"].get("Paused"), "专用测试容器处于暂停状态，请先检查。")
    if not inspected["State"]["Running"]:
        announce("启动已有的专用测试容器：" + CONTAINER)
        capture(["docker", "start", CONTAINER], env)

    deadline = time.monotonic() + 45
    while True:
        result = subprocess.run(["docker", "exec", CONTAINER, "pg_isready"],
                                env=env, capture_output=True, timeout=5)
        if result.returncode == 0:
            break
        require(time.monotonic() < deadline, "PostgreSQL 未在等待时间内就绪。")
        time.sleep(1)
    inspected = json.loads(capture(["docker", "inspect", CONTAINER], env))[0]
    bindings = inspected["NetworkSettings"]["Ports"].get("5432/tcp") or []
    local = [b for b in bindings if b["HostIp"] == "127.0.0.1"]
    require(len(local) == 1, "测试容器没有唯一的 127.0.0.1 PostgreSQL 端口映射。")
    port = int(local[0]["HostPort"])
    require(0 < port < 65536, "数据库端口无效。")
    settings = dict(item.split("=", 1) for item in inspected["Config"]["Env"] if "=" in item)
    username = settings.get("POSTGRES_USER")
    password = settings.get("POSTGRES_PASSWORD")
    require(username and password, "专用容器未提供 POSTGRES_USER/POSTGRES_PASSWORD，无法自动取得连接凭据。")
    databases = capture([
        "docker", "exec", CONTAINER, "sh", "-c",
        'psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc '
        '"SELECT datname FROM pg_database WHERE datname ~ \'^fitnutri_integration_\' AND datallowconn"'
    ], env).splitlines()
    require(len(databases) == 1 and re.fullmatch(r"fitnutri_integration_[a-zA-Z0-9_]+", databases[0]),
            "未找到唯一的 fitnutri_integration_* 基础测试库；为避免误选，请核对专用容器。")
    env.update(TEST_DB_ADMIN_URL="jdbc:postgresql://127.0.0.1:{}/{}".format(port, databases[0]),
               TEST_DB_USERNAME=username, TEST_DB_PASSWORD=password,
               TEST_DB_DOCKER_CONTAINER=CONTAINER)


JDBC_PROBE = r'''
import java.net.*;
import java.sql.*;
class LocalTestConnectionProbe {
    public static void main(String[] args) {
        try {
            Class.forName("org.postgresql.Driver");
            DriverManager.setLoginTimeout(8);
            try (var c = DriverManager.getConnection(System.getenv("TEST_DB_ADMIN_URL"),
                    System.getenv("TEST_DB_USERNAME"), System.getenv("TEST_DB_PASSWORD"));
                 var s = c.createStatement()) {
                s.setQueryTimeout(8);
                if (c.getMetaData().getDatabaseMajorVersion() != 16)
                    throw new IllegalStateException("PostgreSQL 16 required");
                try (var r = s.executeQuery("SELECT rolcreatedb OR rolsuper FROM pg_roles WHERE rolname=current_user")) {
                    if (!r.next() || !r.getBoolean(1))
                        throw new IllegalStateException("Test role requires CREATEDB");
                }
            }
            try (var listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
                if (!listener.isBound()) throw new IllegalStateException("Local HTTP ports unavailable");
            }
            System.out.println("PostgreSQL 16 JDBC connection, credentials, CREATEDB and local ports: OK");
        } catch (Exception e) {
            System.err.println("Connection/permission/port preflight failed: " + e.getClass().getSimpleName());
            if (e instanceof SQLException sql) System.err.println("SQLState: " + sql.getSQLState());
            System.exit(1);
        }
    }
}
'''


def check_connection(env):
    drivers = sorted((Path.home() / ".m2/repository/org/postgresql/postgresql").glob("*/postgresql-*.jar"))
    drivers = [p for p in drivers if not p.name.endswith(("-sources.jar", "-javadoc.jar"))]
    require(drivers, "本机 Maven 缓存缺少 PostgreSQL JDBC 驱动，请先恢复后端依赖。")
    with tempfile.TemporaryDirectory(prefix="fitnutri-connection-check-") as directory:
        source = Path(directory) / "LocalTestConnectionProbe.java"
        source.write_text(JDBC_PROBE)
        result = subprocess.run([str(Path(env["JAVA_HOME"]) / "bin/java"), "--class-path",
                                 str(drivers[-1]), str(source)], cwd=ROOT, env=env, timeout=45)
        require(result.returncode == 0,
                "主机 JDBC 连接检查失败。检查专用容器凭据、端口转发和建库权限。")


def check_fixtures(env):
    source_dir = Path(env.get("TEST_REFERENCE_SOURCE_DIR", str(ROOT / ".local/reference-sources"))).resolve()
    checked = set()
    for manifest in (ROOT / "backend/src/main/resources/data/manifests").glob("*.json"):
        for item in json.loads(manifest.read_text()).get("sourceFiles", []):
            name = item["filename"]
            source = source_dir / name
            require(source.is_file(), "缺少参考源文件：" + name)
            require(hashlib.sha256(source.read_bytes()).hexdigest() == item["sha256"],
                    "参考源 SHA256 不匹配：" + name)
            checked.add(name)
    require(len(checked) >= 5, "未发现完整的参考源校验清单。")
    env["TEST_REFERENCE_SOURCE_DIR"] = str(source_dir)
    ffmpeg = env.get("FFMPEG_BIN", "ffmpeg")
    require(shutil.which(ffmpeg, path=env["PATH"]), "未找到 FFmpeg，请安装后重试。")
    capture([ffmpeg, "-version"], env, timeout=10)
    for name in ("synthetic-tone.webm", "synthetic-tone.mp4"):
        capture([ffmpeg, "-v", "error", "-nostdin", "-i",
                 str(ROOT / "backend/src/test/resources/fixtures/ai" / name),
                 "-f", "null", "-"], env, timeout=15)
    print("参考源 SHA256 与 FFmpeg 实际音频解码检查通过。", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check-only", action="store_true", help="只准备和检查环境，不运行测试")
    parser.add_argument("--verify", action="store_true", help="后端运行 verify（包含额外 IT），默认 test")
    args = parser.parse_args()
    env = os.environ.copy()
    announce("检查 Node/npm、Java/Maven……")
    prepare_runtimes(env)
    announce("准备已有专用 PostgreSQL 容器，并在当前子进程配置连接……")
    prepare_docker(env)
    check_connection(env)
    check_fixtures(env)
    announce("环境预检通过。连接凭据仅用于本次子进程，不写入文件。")
    if args.check_only:
        return 0
    announce("运行前端组件测试（2 个 worker）……")
    subprocess.run(["npm", "--prefix", "frontend", "run", "test:component", "--", "--maxWorkers=2"],
                   cwd=ROOT, env=env, check=True)
    goal = "verify" if args.verify else "test"
    announce("运行后端 Maven " + goal + "……")
    subprocess.run([str(ROOT / "backend/mvnw"), "-f", "backend/pom.xml", goal],
                   cwd=ROOT, env=env, check=True)
    announce("前端组件测试与后端 " + goal + " 均成功结束。")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (CheckError, subprocess.TimeoutExpired, OSError, ValueError, KeyError) as error:
        print("\n环境预检停止：" + str(error), file=sys.stderr)
        sys.exit(1)
    except subprocess.CalledProcessError as error:
        print("\n测试失败，已停止后续执行。请查看上面的测试报告。", file=sys.stderr)
        sys.exit(error.returncode if error.returncode > 0 else 1)
    except KeyboardInterrupt:
        print("\n已中断。", file=sys.stderr)
        sys.exit(130)
