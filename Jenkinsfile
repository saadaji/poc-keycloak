pipeline {
   agent {
       docker {
             image 'maven:3.9.5-eclipse-temurin-17'
             args '--network host -u root -v /var/run/docker.sock:/var/run/docker.sock'
       }
 }
 
   triggers { pollSCM 'H/2 * * * *' } // poll every 2 mins
 
   stages {
       stage('Build and Test') {
           steps {
               sh 'mvn clean install'
           }
       }
   }
}