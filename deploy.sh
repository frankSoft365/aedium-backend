#!/bin/bash

set -e

echo "=========================================="
echo "  Aedium Backend 部署脚本"
echo "=========================================="

check_docker() {
    if ! command -v docker &> /dev/null; then
        echo "[ERROR] Docker 未安装，请先安装 Docker"
        exit 1
    fi
    if ! command -v docker-compose &> /dev/null; then
        echo "[ERROR] Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
    echo "[OK] Docker 和 Docker Compose 已安装"
}

check_maven() {
    if ! command -v mvn &> /dev/null; then
        echo "[ERROR] Maven 未安装，请先安装 Maven"
        exit 1
    fi
    echo "[OK] Maven 已安装"
}

check_github_credentials() {
    echo "[INFO] 检查 GitHub Packages 认证配置..."
    
    MAVEN_CONFIG="$HOME/.m2/settings.xml"
    
    if [ ! -f "$MAVEN_CONFIG" ]; then
        echo "[WARNING] $MAVEN_CONFIG 不存在"
        echo "[INFO] 需要配置 GitHub Packages 认证"
        echo "[INFO] 在 $MAVEN_CONFIG 中添加："
        echo ""
        echo "<settings>"
        echo "  <servers>"
        echo "    <server>"
        echo "      <id>github</id>"
        echo "      <username>frankSoft365</username>"
        echo "      <password>your_github_personal_access_token</password>"
        echo "    </server>"
        echo "  </servers>"
        echo "</settings>"
        echo ""
        read -p "是否现在创建/编辑 settings.xml？(y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            mkdir -p "$HOME/.m2"
            cat > "$MAVEN_CONFIG" << 'EOF'
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>frankSoft365</username>
      <password>your_github_personal_access_token</password>
    </server>
  </servers>
</settings>
EOF
            echo "[OK] settings.xml 已创建，请编辑并填写 personal access token"
            vim "$MAVEN_CONFIG"
        fi
    else
        echo "[OK] settings.xml 已存在"
    fi
}

create_env() {
    if [ -f .env ]; then
        echo "[INFO] .env 文件已存在，跳过创建"
        return
    fi
    
    echo "[INFO] 创建 .env 文件..."
    cp .env.example .env
    echo "[OK] .env 文件已创建，请编辑后重新运行脚本"
    echo ""
    echo "需要填写的配置项："
    echo "  - MYSQL_ROOT_PASSWORD: MySQL 密码"
    echo "  - OSS_ACCESS_KEY_ID: 阿里云 OSS AccessKey ID"
    echo "  - OSS_ACCESS_KEY_SECRET: 阿里云 OSS AccessKey Secret"
    echo "  - FRANK_API_ACCESS_KEY_ID: Frank API AccessKey ID"
    echo "  - FRANK_API_ACCESS_KEY_SECRET: Frank API AccessKey Secret"
    echo ""
    read -p "是否现在编辑 .env 文件？(y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        vim .env
    fi
}

build_project() {
    echo "[INFO] 开始打包项目..."
    mvn clean package -DskipTests
    if [ ! -f target/aedium-backend-0.0.1-SNAPSHOT.jar ]; then
        echo "[ERROR] 打包失败，未找到 jar 文件"
        exit 1
    fi
    echo "[OK] 项目打包成功"
}

start_containers() {
    echo "[INFO] 启动容器..."
    docker-compose up -d
    echo "[OK] 容器启动成功"
}

show_status() {
    echo ""
    echo "=========================================="
    echo "  部署状态"
    echo "=========================================="
    docker-compose ps
    echo ""
    echo "访问地址: http://localhost:8081"
    echo ""
    echo "常用命令:"
    echo "  查看日志: docker-compose logs -f"
    echo "  停止服务: docker-compose down"
    echo "  重启服务: docker-compose restart"
}

main() {
    check_docker
    check_maven
    
    echo ""
    echo "步骤 1/4: 配置 GitHub Packages 认证"
    echo "--------------------------------------"
    check_github_credentials
    
    echo ""
    echo "步骤 2/4: 创建环境变量文件"
    echo "--------------------------------------"
    create_env
    
    echo ""
    read -p "是否继续部署？(y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "[INFO] 部署已取消"
        exit 0
    fi
    
    echo ""
    echo "步骤 3/4: 打包项目"
    echo "--------------------------------------"
    build_project
    
    echo ""
    echo "步骤 4/4: 启动容器"
    echo "--------------------------------------"
    start_containers
    show_status
}

main