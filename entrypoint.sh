#!/bin/bash

# Ejecuta la aplicación Quarkus
java -jar /deployments/quarkus-run.jar

# Espera un poco para que la aplicación se inicie completamente (opcional)
sleep 10

# Elimina los archivos (ajusta los directorios según tus necesidades)
# rm -rf /deployments/lib
# rm -rf /deployments/app
# rm -rf /deployments/quarkus
# rm -f /deployments/quarkus-run.jar

# Cambia al usuario 1000
exec su -s $SHELL -c "exec /bin/bash" 1002

# Mantén el contenedor en ejecución (opcional, para depuración)
#tail -f /dev/null