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
                        $SUDO apt-get install -y podman
                    elif command -v dnf >/dev/null 2>&1; then
                        $SUDO dnf install -y podman
                    elif command -v yum >/dev/null 2>&1; then
                        $SUDO yum install -y podman
                    else
                        echo "No supported package manager found to install Podman"
                        exit 1
                    fi

                    sudo podman --version
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
            }
            steps {
                sh '''

                    export XDG_RUNTIME_DIR="${XDG_RUNTIME_DIR:-/tmp/xdg-runtime-$UID}"
                    mkdir -p "$XDG_RUNTIME_DIR/podman"
                    chmod 700 "$XDG_RUNTIME_DIR"

                    SOCKET_PATH="$XDG_RUNTIME_DIR/podman/podman.sock"
                    if [ ! -S "$SOCKET_PATH" ]; then
                        sudo nohup podman system service --time=0 "unix://$SOCKET_PATH" >/tmp/podman-system-service.log 2>&1 &
                        for i in $(seq 1 20); do
                            [ -S "$SOCKET_PATH" ] && break
                            sleep 1
                        done
                    fi

                    if [ ! -S "$SOCKET_PATH" ]; then
                        echo "Podman socket was not created: $SOCKET_PATH"
                        tail -n 100 /tmp/podman-system-service.log || true
                        exit 1
                    fi

                    export DOCKER_HOST="unix://$SOCKET_PATH"
                    export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="$SOCKET_PATH"

                    sudo podman --version
                    sudo podman info

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
