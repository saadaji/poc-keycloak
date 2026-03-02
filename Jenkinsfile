pipeline {
   agent {
       docker {
             image 'maven:3.9.5-eclipse-temurin-17'
             args '--network host -u root -v /var/run/docker.sock:/var/run/docker.sock'
       }
 }
 
   triggers { pollSCM 'H/2 * * * *' } // poll every 2 mins
 
   stages {
       stage('Checkout Submodules') {
           steps {
               script {
                   // Clone keycloak-email-otp-authenticator if it doesn't exist or is empty
                   sh '''
                       if [ ! -d "keycloak-email-otp-authenticator/.git" ]; then
                           echo "Cloning keycloak-email-otp-authenticator..."
                           rm -rf keycloak-email-otp-authenticator
                           git clone <REPOSITORY_URL> keycloak-email-otp-authenticator
                       fi
                   '''
               }
           }
       }
       stage('Build and Test') {
           steps {
               sh 'mvn clean install'
           }
       }
   }
}