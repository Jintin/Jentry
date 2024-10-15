# Jentry

Jentry is a command line tool to analyze Kotlin/Java public information inside the jar/aar files. 
Can be used to verify exposed API is expected after proguard obfuscation or other modification.

Inspired by [binary-compatibility-validator](https://github.com/Kotlin/binary-compatibility-validator) but using different approach.

## Install
Download the [`Jentry.main.kts`](https://github.com/Jintin/Jentry/blob/master/Jentry.main.kts) file from the repo.

## Usage
`Jentry.main.kts` can be run with `kolin` like `kotlin Jentry.main.kts ...` or directly execute after grant execute permission.

```bash
$ ./Jentry.main.kts <PATH> [-o] [-c]
-o PATH             folder for generate/compare public entries
-c true/false       compare generate files with existing, default false
-h                  show this help message and exit
```

## Contributing
Bug reports and pull requests are welcome on GitHub at [https://github.com/Jintin/Jentry](https://github.com/Jintin/Jentry).

## License
The package is available as open source under the terms of the [MIT License](http://opensource.org/licenses/MIT).

[![Buy Me A Coffee](https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png)](https://www.buymeacoffee.com/jintin)