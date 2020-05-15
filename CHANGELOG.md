# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [0.1.2] - 2020-05-15
### Added
- HTML cleanup fn.

### Changed
- rework map links
- redo UI a bit. Switch to new & simpler site.css. Cleanup.

## [0.1.1] - 2020-05-04
### Changed
- Use localeCompare in JS.
- CSS cleanup and partial minification.
- Tests now use hickory for easier check of output.
- Moved test resources to test/ directory.

### Added
- More meta details in each output page head.
- Generate sitemap.xml
- Full spec validation. Implemented generators.
- Don't rewrite unchanged content to prevent timestamp modification.

### Fixed
- Corrected / improved all tests.
- Delete stale HTML output from categories that no longer have defined restaurants.

## [0.1.0] - 2020-04-20
- Initial release with most features from original site.

[0.1.2]: https://github.com/edporras/nosoup-clj/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/edporras/nosoup-clj/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/edporras/nosoup-clj/releases/tag/v0.1.0
