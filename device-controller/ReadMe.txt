PreRequisites:-

-> Make sure ear is available in user home .m2 directory (or) local repository.

Run Docker Build Script:-

$ ./dockerRun.sh

Run Docker run command:-

$ docker run -p 8080:8080 -p 9990:9990 -d veerabhadrudu/device-controller:latest /opt/jboss/wildfly/bin/standalone.sh -bmanagement 0.0.0.0 -b 0.0.0.0
