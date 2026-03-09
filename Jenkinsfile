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
       stage('Build and Test') {
           steps {
               container('java21') {
                   script {
                       sh "mvn clean install"
                   }
               }
           }
       }
   }

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