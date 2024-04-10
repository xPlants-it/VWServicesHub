#!/bin/sh

cd $( dirname ${0})

rsync -avh --delete-during target/VWServicesHub.woa/ puff:~/WO/VWServicesHub.woa

eval ssh puff "wo-inst /root/WO/VWServicesHub.woa"