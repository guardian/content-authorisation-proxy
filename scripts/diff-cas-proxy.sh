#!/bin/bash

legacyCas=`mktemp`
shinyCas=`mktemp`
mailOutput=`mktemp`

contentType="Content-Type:application/json; charset=UTF-8"
reqBody='{ "appId" : "membership.theguardian.com", "deviceId" : "ROBERTO MADE THIS UP", "subscriberId" : "00381281", "password" : "WA13 9SS"}'

curl -i -H "$contentType" -d "$reqBody" https://content-auth.guardian.co.uk/subs | grep -E -v "Date:|Expires:" > $legacyCas

curl -i --insecure -H "$contentType" -d "$reqBody" https://cas-beta.guardianapis.com/subs | sed s/charset=UTF-8/charset=utf-8/g | grep -E -v "Date:|Expires:|Server:" > $shinyCas


if ! diff $legacyCas $shinyCas; then
	echo "I will report!"

	printf "Subject: Difference between CAS Proxy and CAS\n\nLegacy\n`cat $legacyCas`\n\nProxy\n`cat $shinyCas`" | msmtp roberto.tyley@theguardian.com
fi
