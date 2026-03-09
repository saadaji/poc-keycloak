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
     image: eclipse-temurin:21-jdk-jammy
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
                       sh "./mvnw verify"
                   }
               }
           }
       }
   }
}