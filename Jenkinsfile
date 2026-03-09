def pod =
"""
apiVersion: v1
kind: Pod
metadata:
 labels:
   name: worker
spec:
 serviceAccountName: jenkins
 containers:
   - name: java21
     image: maven:3.9-eclipse-temurin-21
     resources:
       requests:
         cpu: "1000m"
         memory: "2048Mi"
     imagePullPolicy: Always
     tty: true
     command: ["cat"]
   - name: dind
     image: docker:dind
     imagePullPolicy: Always
     tty: true
     env:
       - name: DOCKER_TLS_CERTDIR
         value: ""
     securityContext:
       privileged: true
"""
 
pipeline {
   agent {
       kubernetes {
           yaml pod
       }
   }
   environment {
       DOCKER_HOST = 'tcp://localhost:2375'
       DOCKER_TLS_VERIFY = 0
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
       stage('Build and Test') {
           steps {
               container('java21') {
                   script {
                       sh "mvn clean install"
                   }
               }
           }
       }
       stage('Docker Image Build') {
            steps {
                script{
                    def TAG_VERSION = env.TAG_NAME
                    
                    if (!TAG_VERSION) {
                        TAG_VERSION = input(
                            message: 'Enter tag name for Docker image build:',
                            parameters: [
                                string(name: 'TAG', defaultValue: '', description: 'Docker image tag')
                            ]
                        )
                    }
                    
                    echo "TAG_NAME = ${TAG_VERSION}"

                    withAWS(credentials: "${AWS_USER_ACCOUNT}"){
                        echo keycloak_vef_codebuild_project_name
                        awsCodeBuild projectName: "${keycloak_vef_codebuild_project_name}", region: "${AWS_REGION}", sourceControlType: 'jenkins', credentialsType: 'keys', envVariables: "[{TAG, ${TAG_VERSION}}]"
                    }
                }
            }
        }
   }
}