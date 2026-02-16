pipeline {

    agent {
        label 'maven21'
    }

    parameters {
        string(name: 'TAG_NAME', description: 'TAG used to deploy ex: 17.0.1')
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '15'))
        disableConcurrentBuilds()
        timeout(time: 60, unit: 'MINUTES')
    }

    environment {
        ARTIFACTORY_SERVER = "Artifactory"
        ARTIFACTORY_CREDENTIALS = "technique-plf-keycloak"
    }

    stages {

        stage('Debug') {
            steps {
                echo "BRANCH_NAME = ${env.BRANCH_NAME}"
                echo "TAG_NAME =  ${env.TAG_NAME}"
            }
        }

        stage('Prepare environment') {
            steps {
                script {
                    // variables used to build an deploy dockerfiles to ecr repositories
                    AWS_REGION = 'eu-west-3'
                    AWS_USER_ACCOUNT = "jenkins_techlead_dev"
                    //AWS_ACCOUNT = "529031532506"
                    //ECR_AWS_ACCOUNT = "873168974321"
                    //AWS_ROLE = "PATRIMOINE_SYSOPS_PRI"
                    keycloak_vef_codebuild_project_name = "keycloak-vef-registry-prd"
                }
            }
        }

        stage('Install Podman') {
            steps {
                sh '''
                    if command -v podman >/dev/null 2>&1; then
                        podman --version
                        exit 0
                    fi

                    SUDO=""
                    if [ "$(id -u)" -ne 0 ] && command -v sudo >/dev/null 2>&1; then
                        SUDO="sudo"
                    fi

                    if command -v apt-get >/dev/null 2>&1; then
                        $SUDO apt-get update
                        $SUDO apt-get install -y podman slirp4netns
                    elif command -v dnf >/dev/null 2>&1; then
                        $SUDO dnf install -y podman
                    elif command -v yum >/dev/null 2>&1; then
                        $SUDO yum install -y podman
                    else
                        echo "No supported package manager found to install Podman"
                        exit 1
                    fi

                    $SUDO podman --version
                '''
            }
        }

        stage ('Configuration du serveur Artifactory') {
            steps {
                rtServer (
                    id: "${env.ARTIFACTORY_SERVER}",
                    url: 'https://artifactory.istefr.fr/artifactory',
                    credentialsId: "${env.ARTIFACTORY_CREDENTIALS}",
                    timeout: 300
                )
            }
        }

        stage('Test') {
            environment {
                TESTCONTAINERS_RYUK_DISABLED = 'true'
                TESTCONTAINERS_REUSE_ENABLE = 'true'
                TESTCONTAINERS_CHECKS_DISABLE = 'true'
                TESTCONTAINERS_HOST_OVERRIDE = '127.0.0.1'
                TESTCONTAINERS_DOCKER_CLIENT_PROVIDER_STRATEGY = 'org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy'
                DOCKER_HOST = 'unix:///var/run/docker.sock'
                TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE = '/var/run/docker.sock'
            }
            steps {
                sh '''
                    set -e

                    SUDO=""
                    if [ "$(id -u)" -ne 0 ] && command -v sudo >/dev/null 2>&1; then
                        SUDO="sudo"
                    fi

                    if [ -z "$SUDO" ] && [ "$(id -u)" -ne 0 ]; then
                        echo "sudo is required on this Jenkins agent for podman"
                        exit 1
                    fi

                    if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
                        echo "Using Docker daemon for Testcontainers"
                        docker --version
                        mvn -Dtestcontainers.reuse.enable=true clean test
                        exit 0
                    fi

                    echo "Docker daemon unavailable, using sudo podman service on /var/run/docker.sock"

                    SOCKET_PATH="/var/run/docker.sock"
                    PODMAN_ROOT="/var/tmp/jenkins-podman-root"
                    PODMAN_RUNROOT="/var/tmp/jenkins-podman-runroot"
                    PODMAN_CONTAINERS_CONF="/tmp/jenkins-podman-containers.conf"

                    cat > "$PODMAN_CONTAINERS_CONF" <<'EOF'
[network]
network_backend="netavark"
default_network="host"
default_rootless_network_cmd="slirp4netns"

[containers]
netns="host"
EOF

                    $SUDO mkdir -p /dev/net || true
                    $SUDO modprobe tun || true
                    if ! $SUDO test -c /dev/net/tun; then
                        $SUDO mknod /dev/net/tun c 10 200 || true
                    fi
                    $SUDO chmod 666 /dev/net/tun || true

                    $SUDO mkdir -p "$PODMAN_ROOT" "$PODMAN_RUNROOT"

                    $SUDO pkill -f "podman system service.*$SOCKET_PATH" || true
                    $SUDO rm -f "$SOCKET_PATH" || true

                    if ! $SUDO test -S "$SOCKET_PATH"; then
                        $SUDO env CONTAINERS_CONF="$PODMAN_CONTAINERS_CONF" nohup podman --storage-driver=vfs --root "$PODMAN_ROOT" --runroot "$PODMAN_RUNROOT" system service --time=0 "unix://$SOCKET_PATH" >/tmp/podman-system-service.log 2>&1 &
                        for i in $(seq 1 20); do
                            $SUDO test -S "$SOCKET_PATH" && break
                            sleep 1
                        done
                    fi

                    if ! $SUDO test -S "$SOCKET_PATH"; then
                        echo "Podman socket was not created: $SOCKET_PATH"
                        tail -n 100 /tmp/podman-system-service.log || true
                        exit 1
                    fi

                    $SUDO chmod 666 "$SOCKET_PATH" || true

                    $SUDO podman --version
                    $SUDO env CONTAINERS_CONF="$PODMAN_CONTAINERS_CONF" podman --storage-driver=vfs --root "$PODMAN_ROOT" --runroot "$PODMAN_RUNROOT" info

                    echo "DOCKER_HOST=$DOCKER_HOST"
                    echo "TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=$TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE"
                    [ -S "${DOCKER_HOST#unix://}" ] || { echo "Docker socket does not exist"; exit 1; }

                    mvn -Dtestcontainers.reuse.enable=true clean test
                '''
            }
        }

        stage('Maven Build') {
            steps {
                script {
                    echo "Building Maven project..."
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        /*stage('Download jar from Artifactory') {
            when {
                expression { return env.TAG_NAME }
            }
            steps {
                rtDownload (
                    serverId: "${env.ARTIFACTORY_SERVER}",
                    spec: """{
                          "files": [
                            {
                                "pattern": "istefr/plf/keycloak/habilia_protocol_map/habilia_protocol_map_20220613.3.jar",
                                "target": "keycloak-image/providers/",
                                "flat": "true"
                            }
                         ]
                    }"""
                )
            }

        }*/

        stage('Docker Image Build') {
            when {
                expression { return env.TAG_NAME }
            }
            steps {
                script{
                    echo "TAG_NAME =  ${env.TAG_NAME }"

                    withAWS(credentials: "${AWS_USER_ACCOUNT}"){
                        echo keycloak_vef_codebuild_project_name
                        awsCodeBuild projectName: "${keycloak_vef_codebuild_project_name}", region: "${AWS_REGION}", sourceControlType: 'jenkins', credentialsType: 'keys', envVariables: "[{TAG, $env.TAG_NAME }]"
                    }
                }
            }
        }
    }
}
