# clock-in

A Clojure program to automatically update time-watches coming and going hours.

## Usage

Download the jar and run it.
```java -jar clock-in.jar -u <USER-NUM> -p <PASSWORD> -c <COMPANY-ID>```
or, in case you watch Netflix instead of going to work in the morning/bla bla bla and come to work at 12:30
```java -jar clock-in.jar --michael -u <USER-NUM> -p <PASSWORD> -c <COMPANY-ID>```

No args will provide the a help string.

Filled times are 8:30am - 17:30 Sunday-Thursday, unless michael, in that case 12:30-21:30.
Filled month is current unless using the ```--next-month``` or ```--previous-month``` flags.

## License

Copyright © 2018 Gargamel

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
