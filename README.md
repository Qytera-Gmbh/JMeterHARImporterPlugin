# JMeter HAR Importer Plugin

The JMeter HAR Importer plugin allows users to import HTTP Archive (HAR) files into Apache JMeter. The imported HAR files are then used to create JMeter sampler elements, facilitating the conversion of HAR files into JMeter test plans.

## Installation

To install the JMeter HAR Importer plugin, follow these steps:

1. Download the latest release of the plugin JAR file from the [GitHub releases page](https://github.com/Qytera-Gmbh/JMeterHARImporterPlugin/releases).
2. Copy the downloaded JAR file to the "lib/ext" directory within your JMeter installation directory.
3. Restart JMeter to load the plugin.

## Usage

Once installed, follow these steps to import a HAR file into JMeter:

1. Launch JMeter.
2. Go to the "Tools" menu.
3. Click on "Import HAR File".
4. In the dialog box that appears, browse and select the HAR file you want to import.
5. Click "Import".
6. The plugin will automatically create HTTP Sampler requests for each entry in the HAR file, along with necessary configurations such as Header Managers and Cookie Managers.

## Contributing

Contributions to the JMeter HAR Importer plugin are welcome! If you encounter any bugs or have feature requests, please submit an issue on the [GitHub repository](https://github.com/Qytera-Gmbh/JMeterHARImporterPlugin/issues).

## License

This plugin is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

# Deploy and Test

Execute `run.bat`, maybe the path to your JMeter installation must be adapted to the right location `-Djmeter.path=C:/scoop/apps/jmeter/current`.
