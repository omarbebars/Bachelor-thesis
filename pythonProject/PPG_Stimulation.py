import neurokit2 as nk
import matplotlib.pyplot as plt
import time
import numpy as np

# Parameters
duration = 120  # Total duration of the signal in seconds
sampling_rate = 100  # Sampling rate in Hz
window_size = 1 # Size of the moving window in seconds
update_interval = 0.05  # Update interval in seconds

# Simulate PPG signal
ppg_signal = nk.ppg_simulate(duration=duration, sampling_rate=sampling_rate, heart_rate=120)
print(ppg_signal)

# Limit to first 1000 values
ppg_signal = ppg_signal[:1000]

# Save PPG signal to a text file
output_file = 'ppg_values.txt'
np.savetxt(output_file, ppg_signal, fmt='%.8f')  # Save values with 8 decimal places
print(f"PPG signal values have been written to {output_file}.")

# Function to format the PPG signal for Arduino
def format_array_for_arduino(array, values_per_line=10):
    formatted_array = "{\n    "
    for i, value in enumerate(array):
        formatted_array += f"{value:.8f}, "
        if (i + 1) % values_per_line == 0:
            formatted_array += "\n    "
    formatted_array = formatted_array.rstrip(", ") + "\n};"
    return formatted_array

# Format the PPG signal
formatted_ppg = format_array_for_arduino(ppg_signal, values_per_line=10)
print("Formatted PPG Signal for Arduino:")
print(formatted_ppg)

# figure for plotting
plt.ion()  
fig, ax = plt.subplots()
xdata, ydata = [], []
ln, = plt.plot([], [], 'b-')
plt.ylim(-2, 2)
plt.xlabel('Time (s)')
plt.ylabel('Amplitude')

start = 0
window_samples = window_size * sampling_rate

# Main loop for updating the plot
try:
    while start + window_samples <= len(ppg_signal):
        end = start + window_samples
        xdata = [x / sampling_rate for x in range(start, end)]
        ydata = ppg_signal[start:end]

        ln.set_data(xdata, ydata)
        plt.xlim(xdata[0], xdata[-1])
        plt.draw()
        plt.pause(update_interval)

        start += int(update_interval * sampling_rate)
        time.sleep(update_interval)  # Pause for a real-time effect

except Exception as e:
    print(f"An error occurred: {e}")

plt.ioff()  # Disable interactive mode
plt.show()