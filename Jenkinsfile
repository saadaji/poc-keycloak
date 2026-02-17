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

        stage('Setup Podman') {
            steps {
                sh '''
                    set -e
                    
                    if ! command -v podman >/dev/null 2>&1; then
                        echo "Installing Podman..."
                        if command -v apt-get >/dev/null 2>&1; then
                            sudo apt-get update
                            sudo apt-get install -y podman slirp4netns
                        elif command -v yum >/dev/null 2>&1; then
                            sudo yum install -y podman
                        else
                            echo "ERROR: Cannot install Podman - no supported package manager found"
                            exit 1
                        fi
                    fi
                    
                    echo "Podman version:"
                    podman --version
                    
                    echo "Setting up /dev/net/tun..."
                    sudo mkdir -p /dev/net || true
                    sudo modprobe tun 2>/dev/null || echo "Could not load tun module"
                    if [ ! -c /dev/net/tun ]; then
                        sudo mknod /dev/net/tun c 10 200 2>/dev/null || echo "Could not create /dev/net/tun"
                    fi
                    sudo chmod 666 /dev/net/tun 2>/dev/null || true
                    
                    echo "Setting up Podman service for Testcontainers..."
                    SOCKET_PATH="/var/run/docker.sock"
                    PODMAN_ROOT="/var/lib/jenkins-podman"
                    PODMAN_RUNROOT="/var/run/jenkins-podman"
                    
                    sudo mkdir -p "$PODMAN_ROOT" "$PODMAN_RUNROOT" /var/run
                    sudo pkill -f "podman system service" 2>/dev/null || true
                    sudo rm -f "$SOCKET_PATH" 2>/dev/null || true
                    
                    cat > /tmp/podman-jenkins.conf <<'EOF'
[network]
network_backend="slirp4netns"

[containers]
netns="host"
userns="host"
default_sysctls = []

[engine]
cgroup_manager="cgroupfs"
events_logger="file"
EOF
                    
                    echo "Attempting to set sysctl for route_localnet (may fail in restricted environments)..."
                    sudo sysctl -w net.ipv4.conf.all.route_localnet=1 2>/dev/null || echo "Could not set route_localnet - continuing with slirp4netns"
                    
                    echo "Starting Podman service..."
                    sudo env CONTAINERS_CONF=/tmp/podman-jenkins.conf NETAVARK_DISABLE_ROUTE_LOCALNET=1 nohup podman \
                        --storage-driver=vfs \
                        --root "$PODMAN_ROOT" \
                        --runroot "$PODMAN_RUNROOT" \
                        system service --time=0 "unix://$SOCKET_PATH" \
                        >/tmp/podman-service.log 2>&1 &
                    
                    echo "Waiting for Podman socket..."
                    for i in $(seq 1 30); do
                        if [ -S "$SOCKET_PATH" ]; then
                            echo "Podman socket created"
                            sudo chmod 666 "$SOCKET_PATH"
                            break
                        fi
                        sleep 1
                    done
                    
                    if [ ! -S "$SOCKET_PATH" ]; then
                        echo "ERROR: Podman socket not created"
                        tail -n 50 /tmp/podman-service.log 2>/dev/null || true
                        exit 1
                    fi
                    
                    echo "Testing Podman connection..."
                    if podman --remote --url "unix://$SOCKET_PATH" info >/dev/null 2>&1; then
                        echo "Podman service is ready"
                    else
                        echo "WARNING: Podman service may not be fully ready"
                        tail -n 20 /tmp/podman-service.log 2>/dev/null || true
                    fi
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
            when {
                expression {
                    // Only run tests if we have a working container runtime (Docker or Podman)
                    return fileExists('/var/run/docker.sock') && sh(script: 'podman --remote --url unix:///var/run/docker.sock info >/dev/null 2>&1', returnStatus: true) == 0
                }
            }
            environment {
                TESTCONTAINERS_RYUK_DISABLED = 'true'
                TESTCONTAINERS_REUSE_ENABLE = 'true'
                DOCKER_HOST = 'unix:///var/run/docker.sock'
            }
            steps {
                sh '''
                    set -e

                    if [ ! -S /var/run/docker.sock ]; then
                        echo "ERROR: Docker-compatible socket not found at /var/run/docker.sock"
                        echo "The Setup Podman stage should have created this socket."
                        ls -la /var/run/ | grep -E '(docker|podman)' || echo "No docker/podman sockets found"
                        exit 1
                    fi

                    echo "Docker-compatible socket found, running tests..."
                    ls -l /var/run/docker.sock

                    mvn -Dtestcontainers.reuse.enable=true clean test || {
                        echo "WARN: Tests failed - this may be due to container runtime restrictions on this agent"
                        echo "Consider running tests on a Docker-capable agent or skipping Testcontainers tests"
                        exit 1
                    }
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
