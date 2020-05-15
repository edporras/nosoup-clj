# nosoup-clj v0.1.2

Site generator for https://www.nosoupforyou.com/, a listing of local &
independent restaurants in Gainesville, FL I created in the early
2000's. The original project was written with PHP/MySQL and over the
past 8 or so years, it'd been running on AWS. I decided to move away
from all that and rewrote the whole thing as a Clojure app that
generates static pages. Instead of a database, I'm using a simple EDN
configuration.

## Usage

I can't imagine anyone will want to do this but, assuming you've
created your own configuration, you generate the static pages with:

    $ java -jar nosoup-clj-0.1.2-standalone.jar gen path-to-restaurants-config.edn

See [test/restaurants.edn](test/restaurants.edn) for an example.
