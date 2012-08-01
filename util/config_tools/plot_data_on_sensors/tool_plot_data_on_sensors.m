function []=tool_plot_data_on_sensors(filename,data)

figure
sensors = tool_extractsensorinfo(filename);
scatter([sensors.lng],[sensors.lat],50,data,'filled'), hold on
scatter([sensors.lng],[sensors.lat],50,[0 0 0])
plot_google_map('maptype','roadmap')
