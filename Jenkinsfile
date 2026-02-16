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

        stage('Install Docker') {
            steps {
                sh '''
                    set -e

                    SUDO=""
                    if [ "$(id -u)" -ne 0 ]; then
                        if ! command -v sudo >/dev/null 2>&1; then
                            echo "sudo is required to install docker on this Jenkins agent."
                            exit 1
                        fi
                        SUDO="sudo"
                    fi

                    if command -v docker >/dev/null 2>&1; then
                        echo "Docker CLI already installed"
                    else
                        if command -v apt-get >/dev/null 2>&1; then
                            $SUDO apt-get update
                            $SUDO apt-get install -y docker.io
                        elif command -v dnf >/dev/null 2>&1; then
                            $SUDO dnf install -y docker
                        elif command -v yum >/dev/null 2>&1; then
                            $SUDO yum install -y docker
                        else
                            echo "No supported package manager found to install docker"
                            exit 1
                        fi
                    fi

                    if command -v systemctl >/dev/null 2>&1; then
                        $SUDO systemctl start docker || true
                        $SUDO systemctl enable docker || true
                    elif command -v service >/dev/null 2>&1; then
                        $SUDO service docker start || true
                    fi

                    if ! $SUDO docker info >/dev/null 2>&1; then
                        echo "Docker service manager unavailable or failed; starting dockerd manually"
                        if ! command -v dockerd >/dev/null 2>&1; then
                            echo "dockerd binary not found on this Jenkins agent"
                            exit 1
                        fi

                        $SUDO mkdir -p /var/lib/jenkins-docker
                        $SUDO mkdir -p /var/run
                        $SUDO rm -f /var/run/docker.sock || true
                        $SUDO nohup dockerd \
                            --host=unix:///var/run/docker.sock \
                            --data-root=/var/lib/jenkins-docker \
                            --storage-driver=vfs \
                            --iptables=false \
                            --bridge=none \
                            --ip-masq=false \
                            --ip-forward=false \
                            >/tmp/dockerd.log 2>&1 &

                        for i in $(seq 1 30); do
                            if $SUDO docker info >/dev/null 2>&1; then
                                break
                            fi
                            sleep 1
                        done
                    fi

                    $SUDO docker --version
                    if ! $SUDO docker info >/dev/null 2>&1; then
                        echo "Docker daemon could not be started on this Jenkins agent."
                        tail -n 200 /tmp/dockerd.log || true
                        exit 1
                    fi

                    $SUDO chmod 666 /var/run/docker.sock || true
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
                TESTCONTAINERS_DOCKER_CLIENT_PROVIDER_STRATEGY = 'org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy'
                DOCKER_HOST = 'unix:///var/run/docker.sock'
                TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE = '/var/run/docker.sock'
            }
            steps {
                sh '''
                    set -e

                    SUDO=""
                    if [ "$(id -u)" -ne 0 ]; then
                        if ! command -v sudo >/dev/null 2>&1; then
                            echo "sudo is required to run docker on this Jenkins agent."
                            exit 1
                        fi
                        SUDO="sudo"
                    fi

                    if ! command -v docker >/dev/null 2>&1; then
                        echo "Docker CLI is not installed on this Jenkins agent."
                        exit 1
                    fi

                    if [ ! -S /var/run/docker.sock ]; then
                        echo "Docker socket /var/run/docker.sock is not available on this Jenkins agent."
                        exit 1
                    fi

                    if ! $SUDO docker info >/dev/null 2>&1; then
                        echo "Docker daemon is not reachable with sudo on this Jenkins agent."
                        exit 1
                    fi

                    $SUDO chmod 666 /var/run/docker.sock || true

                    $SUDO docker --version
                    $SUDO docker info > /dev/null

                    if ! docker info >/dev/null 2>&1; then
                        echo "Docker daemon is reachable with sudo but not by Jenkins user."
                        ls -l /var/run/docker.sock || true
                        exit 1
                    fi

                    DOCKER_HOST="unix:///var/run/docker.sock" \
                    TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="/var/run/docker.sock" \
                    TESTCONTAINERS_DOCKER_CLIENT_PROVIDER_STRATEGY="org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy" \
                    TESTCONTAINERS_RYUK_DISABLED="true" \
                    TESTCONTAINERS_REUSE_ENABLE="true" \
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
