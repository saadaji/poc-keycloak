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
                    set -e

                    if ! command -v docker >/dev/null 2>&1; then
                        echo "ERROR: Docker is not installed on maven21 agent."
                        echo "Please install Docker on the maven21 Jenkins agent or configure a Docker-enabled agent."
                        exit 1
                    fi

                    if ! docker info >/dev/null 2>&1; then
                        echo "ERROR: Docker daemon is not running or not accessible on maven21 agent."
                        echo "Ensure Docker daemon is running and Jenkins user has socket access (add to 'docker' group or set socket permissions)."
                        exit 1
                    fi

                    docker --version
                    docker info

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
