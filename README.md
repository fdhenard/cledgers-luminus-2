# cledgers

[toc]

generated using Luminus version "4.10"

## How it was generated

    $ cd ~/dev/repos/
    $ mkdir cledgers-luminus-2
    $ lein new luminus cledgers --to-dir . --force -- +jetty +postgres +re-frame +shadow-cljs +auth

FIXME

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

- nvm + latest LTS node version
- frontend dev install

    ```
    cd ~/dev/repos/cledgers-luminus-2
    npm install
    ```

- postgresql

## Running for dev

### backend

To start a web server for the application, run:

#### from repl

```
user> (mount/start)
```

#### from terminal

```
cd ~/dev/repos/cledgers-luminus-2
lein run
```

### frontend

```
cd ~/dev/repos/cledgers-luminus-2
nvm use stable # 7/9/21 v14.17.1
npx shadow-cljs watch app
```

## License

Copyright © 2021 Frank Henard - Eclipse like Clojure
