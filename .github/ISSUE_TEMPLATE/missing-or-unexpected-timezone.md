---
name: Missing or unexpected timezone
about: Use this if Timeshape query returns unexpected or no timezone
title: Missing or unexpected timezone
labels: ''
assignees: ''

---

# Wrong or unexpected timezone is returned

The most frequent reason for wrong or missing timezone id in Timeshape response is an outdated version of timezone database (`tzdb`) in Java distribution. To help you figure out why it doesn't work, please answer the following questions:

## Which Timeshape version are you using?
E.g. 2022g.16

## Which Java version are you using?
E.g. 8.0.361, 11.0.8 or 17.0.6.

## If you upgrade to the latest Java version, does it still fail?
  * Yes 
    - Which version have you tried?
  *  No
    - Then there's no issue!

## Which coordinates are you using?
Please use `lat, lon` format, e.g. 61.237, 13.801. You may specify multiple coordinate pairs.

## Which timezone is returned?
Please specify a full timezone id, such as `Europe/Berlin` or `Asia/Krasnoyarsk`. If no timezone is returned, please use `None`.

## Which timezone is expected?
Please use the same format as above.

## Additional information
E.g. if this used to work in some previous Timeshape version, or any other important facts.
