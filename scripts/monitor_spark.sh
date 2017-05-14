#!/usr/bin/env bash
echo Use to crate CSV file: 'cat sparkapp_perf.log | jq -s '.' | in2csv -f json'
echo
echo Starting

appid=$(curl http://localhost:4040/api/v1/applications | jq 'first | .id' | tr -d '"')

function collect() {
    jobinfo=$(curl http://localhost:4040/api/v1/applications/$appid/jobs --max-time 5 | \
      jq '.[] | {"jobId", "status", "name"} | select (.status | contains("RUNNING"))')

    cpuper="{\"CPU%\": \"$(top -bc -n 1 | grep spark | grep java  | awk '{print $9}' | head -n 1)\"}"
    memper="{\"Mem%\": \"$(top -bc -n 1 | grep spark | grep java  | awk '{print $10}' | head -n 1)\"}"
    disksize="{\"disk size\": $(du -sm data/models/ | cut -f 1)}"
    ioper=$(sudo iotop -b -n 1 | grep spark | awk '{io+=$8} END {printf "{\"IO%\": \"%.2f\"}", io}' )

    #echo $jobinfo
    #echo $cpuper
    #echo $disksize
    #echo $ioper

    jq -s '.[0] * .[1] * .[2] * .[3] * .[4] * .[5]' \
      <(echo $cpuper) \
      <(echo $memper) \
      <(echo ${jobinfo:-'{"jobId":"", "status":"", "name":""}'}) \
      <(echo "{\"time\": \"$(date)\"}") \
      <(echo $ioper) \
      <(echo $disksize)

}

# TODO jstack $(top -bc -n 1 | grep spark | grep java | awk '{print $1 }')

while [ true ]
do
    collect >> sparkapp_perf.log
    sleep 10
done
