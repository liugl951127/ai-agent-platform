# =====================================================================
# AI Agent Platform - Spring Boot 微服务统一 Dockerfile
# 用法:
#   docker build --build-arg MODULE=agent-llm -t agent-llm:latest -f Dockerfile .
# 或:  docker build -f agent-llm/Dockerfile .   (子目录的简化版)
# =====================================================================

# ---- Stage 1: 构建 ----
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

# 先复制父 pom,利用 Docker 缓存
COPY pom.xml ./
COPY agent-common/pom.xml ./agent-common/
COPY agent-gateway/pom.xml ./agent-gateway/
COPY agent-auth/pom.xml ./agent-auth/
COPY agent-system/pom.xml ./agent-system/
COPY agent-llm/pom.xml ./agent-llm/
COPY agent-workflow/pom.xml ./agent-workflow/
COPY agent-knowledge/pom.xml ./agent-knowledge/
COPY agent-conversation/pom.xml ./agent-conversation/
COPY agent-agent/pom.xml ./agent-agent/

# 预下载依赖(只要 pom 没变就走缓存)
RUN mvn -B -e -ntp -q dependency:go-offline -pl agent-common,${MODULE} -am || true

# 再复制源码
COPY agent-common ./agent-common
COPY ${MODULE} ./${MODULE}
RUN mvn -B -e -ntp -q clean package -DskipTests -pl ${MODULE} -am \
    && cp ${MODULE}/target/${MODULE}-*.jar /app.jar

# ---- Stage 2: 运行时 ----
FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="liugl951127 <https://github.com/liugl951127>"
WORKDIR /app

# 时区 + 中文
ENV TZ=Asia/Shanghai
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Dfile.encoding=UTF-8"

# 阿里云镜像加速(国内环境)
RUN sed -i 's|dl-cdn.alpinelinux.org|mirrors.aliyun.com|g' /etc/apk/repositories \
    && apk add --no-cache tzdata curl \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

COPY --from=build /app.jar /app/app.jar

EXPOSE 9000

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:9000/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
