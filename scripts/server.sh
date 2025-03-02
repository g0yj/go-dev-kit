#!/bin/bash

# 애플리케이션 소스 디렉토리
SOURCE_DIR="/home/ec/kowoontech/englishchannel-server"
LOG_DIR="/home/ec/kowoontech/logs"
DUMP_DIR="$LOG_DIR/dump"

# 로그 및 힙 덤프 디렉토리 생성
mkdir -p "$LOG_DIR"
mkdir -p "$DUMP_DIR"

# 스크립트의 디렉토리로 이동
cd "$SOURCE_DIR" || { echo "Failed to change directory to $SOURCE_DIR. Exiting."; exit 1; }

# 명령줄 인자로 선택적 동작 제어
DO_PULL=false
DO_BUILD=false
DO_KILL=false

# 인자 파싱
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --pull) DO_PULL=true ;;
        --build) DO_BUILD=true ;;
        --kill) DO_KILL=true ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
    shift
done

# Git 리포지토리 업데이트
if [ "$DO_PULL" = true ]; then
    echo "Pulling latest changes from the main branch..."
    git checkout main
    git pull origin main
else
    echo "Skipping git pull."
fi

# Maven Clean 및 빌드 (테스트 생략)
if [ "$DO_BUILD" = true ]; then
    echo "Cleaning the project with Maven..."
    ./mvnw clean

    echo "Building the project with Maven (Skipping integration tests)..."
    ./mvnw install -DskipTests -DskipITs

    # 빌드 결과 확인
    if [ $? -ne 0 ]; then
        echo "Maven build failed. Exiting."
        exit 1
    fi
else
    echo "Skipping Maven build."
fi

# 기존 서버 프로세스 강제 종료
if [ "$DO_KILL" = true ]; then
    echo "Killing existing Spring Boot application..."
    APP_PIDS=$(ps aux | grep '[j]ava' | awk '{print $2}')

    if [ -n "$APP_PIDS" ]; then
        echo "Found Java processes: $APP_PIDS"
        echo "$APP_PIDS" | xargs kill -9
        echo "Java processes killed."
    else
        echo "No Java processes found."
    fi

    # kill 후 스크립트 종료
    echo "Server processes terminated. Exiting script."
    exit 0
fi

# 기존의 서버 프로세스가 있는지 확인하고 종료
echo "Stopping existing Spring Boot application gracefully..."
APP_PIDS=$(ps aux | grep '[j]ava' | awk '{print $2}')

if [ -n "$APP_PIDS" ]; then
    echo "Found Java processes: $APP_PIDS"
    echo "$APP_PIDS" | xargs kill
    sleep 5  # Graceful 종료 대기
    if ps -p "$APP_PIDS" > /dev/null; then
        echo "Java processes still running. Forcefully killing them..."
        echo "$APP_PIDS" | xargs kill -9
    else
        echo "Java processes stopped gracefully."
    fi
else
    echo "No Java processes found."
fi

# Spring Boot 애플리케이션 실행 (prod 프로파일)
echo "Starting Spring Boot application with 'prod' profile..."

# JVM 옵션 설정 (힙 덤프, GC 로그, 쓰레드 덤프)
JVM_OPTS="-Xms2g -Xmx4g -XX:MaxMetaspaceSize=512m"
JVM_OPTS="$JVM_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$DUMP_DIR"
JVM_OPTS="$JVM_OPTS -Xlog:gc*:file=$LOG_DIR/gc.log:time,tags:filecount=5,filesize=10M"
JVM_OPTS="$JVM_OPTS -Dfile.encoding=UTF-8"
JVM_OPTS="$JVM_OPTS -XX:InitiatingHeapOccupancyPercent=45 -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=16m"

# 애플리케이션 실행
nohup java $JVM_OPTS -jar target/*.jar --spring.profiles.active=prod > "$LOG_DIR/server.log" 2>&1 &

# 애플리케이션 실행 결과 확인
if [ $? -ne 0 ]; then
    echo "Failed to start Spring Boot application. Exiting."
    exit 1
fi

echo "Spring Boot application started successfully. Check logs in $LOG_DIR/server.log."

# 실시간 로그 모니터링
touch "$LOG_DIR/server.log"
echo "Tail application logs:"
tail -f "$LOG_DIR/server.log"