# Statistical Verification Framework of Platooning SoS

The framework is to effectively verify the existing Platooning management system, VENTOS for the case study of applying fault localization and diagnosis technique on the CPS. The remaining part contains 1) Installation of the StarPlateS framework (ver. OCT 2019).

## Installation

The first step to use the StarPlateS is installing the VENTOS framework on your device. Because the VENTOS is optimized on the Ubuntu 16.04 64bit, MAC Sierra(10.12), Elcapitan(10.11), and Yosemite(10.10), we strongly recommend you to use one of the OS versions. 
The following installation steps are based on the Ubuntu 16.04 64bit.

### Installing the VENTOS framework

1. Download the git
<pre><code> sudo apt-get install git </code></pre>

2. Clone the VENTOS_Public repository
I downloaded the repository at Desktop to follow the same configuration in the VENTOS manual.
<pre><code> cd ~/Desktop
 git clone https://github.com/ManiAm/VENTOS_Public
</code></pre>

3. Execute runme script in the VENTOS_Public folder.
You should not execute the "runme" script as sudo/root. 
It may take 1~2 hour to check and install the essential libraries. 
<pre><code> cd VENTOS_Public
 ./runme
</code></pre>
There could be an error with downloading "omnetpp-5.4.1-src-linux-tgz" file. In that case, you can download the file at [OMNETT++5.4.1](https://omnetpp.org/download/old.html), and extract it at Desktop. After that, you can execute "runme" successfully. 
If you get any other error messages when installing libraries, please refer the [VENTOS Issues](https://github.com/ManiAm/VENTOS_Public/issues) to solve the error. 

## Reference
1. https://maniam.github.io/VENTOS/
