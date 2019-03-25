ARM MCU development plugin for JetBrains CLion 
====

***:point_up: Please Note!***

*This plugin was integrated in CLion 2019.1, and it will be further developed as part of the IDE. Older versions of the 
plugin will remain available in the [plugins repository](https://plugins.jetbrains.com/plugin/10115).*

*Refer to the [press release](https://blog.jetbrains.com/clion/2019/02/clion-2019-1-eap-clion-for-embedded-development-part-iii/) for more information.*

*For bug reports and feature requests, please use 
[JetBrains YouTrack](https://youtrack.jetbrains.com/newIssue?project=CPP&description=Clion%20Version%3A%0AOpenOCD%20Version%3A%0AToolchain%20Version%3A%0ATarget%20MCU%2Fboard%3A&c=Subsystem%20Embedded).
Feel free to upvote and comment in [exising tickets](https://youtrack.jetbrains.com/issues?q=Subsystem:%20Embedded%20sort%20by:%20State%20).*

*This repository was archived and turned read-only.*

---

[![Join the chat at https://gitter.im/clion-embedded-arm/Lobby](https://badges.gitter.im/clion-embedded-arm/Lobby.svg)](https://gitter.im/clion-embedded-arm/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Potentially you can use the plugin to work with any kind of MCU supported by GCC and OpenOCD, but major auditory for 
the plugin are STM32 developers. 

Plugin page at [Jetbrains Repository](https://plugins.jetbrains.com/plugin/10115)

Blog post at [CLion blog](https://blog.jetbrains.com/clion/2017/12/clion-for-embedded-development-part-ii)

![Screenshot](screen1.png)

The plugin is able to:
---
 * Convert a project made in *[STM32CubeMX](http://www.st.com/en/development-tools/stm32cubemx.html)* into a  *[CLion](https://www.jetbrains.com/clion/)* project. The project tested against CLion 2018.3 EAP.
 * Download project binaries into compatible MCU using *[OpenOCD](http://openocd.org/)*
 * Debug project on chip
 

Disclaimer
---
 * No warranties, you are using the plugin at your own risk.
 * Beware bugs! This is very early version.

License
---
[MIT](LICENSE.txt)

How To Use
---
See [USAGE.md](USAGE.md).

Contributions
===
First of all, please have a look at our [code of conduct](CODE_OF_CONDUCT.md). Well, it's standard stuff, I believe you won't do wrong things. Then read our [contribution guide](CONTRIBUTING.md). 

Likes and Donations
===

If you like the plugin, you may :star: the project at github (button at top-right of the page) and at [jetbrains plugins repository](https://plugins.jetbrains.com/plugin/10115).

The plugin is free<s>, but you can support my work with a donation</s>. 

<s>10 EUR | 20 EUR | Other amount</s>

Thanks to all the donators, you gave me some resources, and a very important feeling that I am doing a good thing. Now I feel I can not ask for donations anymore. If you still have the intention to support free stuff for embedded development, you may donate [OpenOCD.org](http://openocd.org/donations/) project, those guys are doing a great job for all of us. 

[Hall of Donators](DONATIONS.md)
--
