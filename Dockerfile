FROM alpine:3.20

COPY build/libs/rdc-fulfillment-extension-*.jar /etc/extensions/fulfillment/
COPY extensions.properties                      /etc/extensions/fulfillment/

# Remove files left by a previous version first, so the fulfillment service never loads two jars
CMD rm -f /extensions/* && cp /etc/extensions/fulfillment/* /extensions/
