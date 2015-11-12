lein clean && lein cljsbuild once min
cp -r resources/public /tmp/
git checkout gh-pages
cp -r /tmp/public/* .
git commit -am "automatic update"
