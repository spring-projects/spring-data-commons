pipeline {
    agent {
    	docker {
    		image 'maven:3.3.3'
		}
	}
    stages {
        stage('build') {
            steps {
                sh 'mvn clean dependency:list test -Dsort -U'
            }
        }
    }
}