import random

mapping = {
    -2:'00',
    -1:'01',
    1:'10',
    2:'11',
    -6:'000',
    -5:'001',
    -4:'010',
    -3:'011',
    3:'100',
    4:'101',
    5:'110',
    6:'111',
    14: '1111',
    13: '1110',
    12: '1101',
    11: '1100',
    10: '1011',
    9: '1010',
    8: '1001',
    7: '1000',
    -14: '0000',
    -13: '0001',
    -12: '0010',
    -11: '0011',
    -10: '0100',
    -9: '0101',
    -8: '0110',
    -7: '0111',
    30: '11111',
    29:'11110',
    28:'11101',
    27:'11100',
    26:'11011',
    25:'11010',
    24:'11001',
    23:'11000',
    22:'10111',
    21:'10110',
    20:'10101',
    19:'10100',
    18:'10011',
    17:'10010',
    16:'10001',
    15:'10000',
    -30:'00000',
    -29:'00001',
    -28:'00010',
    -27:'00011',
    -26:'00100',
    -25:'00101',
    -24:'00110',
    -23:'00111',
    -22:'01000',
    -21:'01001',
    -20:'01010',
    -19:'01011',
    -18:'01100',
    -17:'01101',
    -16:'01110',
    -15:'01111',
}
def difference(diff):
    if (diff == 0):
        return None
    elif (diff >= -2 and diff <= -1) or (diff >= 1 and diff <= 2):
        return '00 00 ' + mapping[diff]
    elif (diff >= -6 and diff <= -3) or (diff >= 3 and diff <= 6):
        return '00 01 ' + mapping[diff]
    elif (diff >= -14 and diff <= -7) or (diff >= 7 and diff <= 14):
        return '00 10 ' + mapping[diff]
    elif (diff >= -30 and diff <= -15) or (diff >= 15 and diff <= 30):
        return '00 11 ' + mapping[diff]
    else:
        abs_diff = abs(diff)
        sign_bit = '0' if diff > 0 else '1'
        return '10 ' + sign_bit + format(abs_diff, '08b')

def repetitions(count):
    return '01 ' + format(count-1, '03b')

def number(n):
    return format(n, '08b')

def compress(numbers):
    encoded_string = number(numbers[0])
    i = 1

    while i < len(numbers):
        diff = numbers[i] - numbers[i - 1]
        count = 1
        while i + 1 < len(numbers) and numbers[i + 1] - numbers[i] == diff:
            i += 1
            count += 1
        
        encoded_diff = difference(diff)
        if encoded_diff:
            encoded_string += encoded_diff
        
        if count > 1:
            encoded_string += repetitions(count)

        if count == 1 and diff == 0:
            encoded_string+='01 000'
        
        i += 1
    
    encoded_string += '11'
    return encoded_string.replace(' ', '')


def writeToFile(compressed, file_name):
    file_path = file_name
    bytes_to_write = compressed.encode('utf-8')
    with open(file_path, 'wb') as file:
        file.write(bytes_to_write)

def compress_matrix(matrix):
    compressed_matrix = ""
    for row in matrix:
        # Skip the row if it's empty
        if not row:
            continue
        compressed_row = compress(row)
        compressed_matrix += compressed_row + "\n"  # Separate rows by newline
    return compressed_matrix.strip()

def parse_matrix(file_path):
    matrix = []
    start_reading = False
    with open(file_path, 'r') as file:
        for line in file:
            if start_reading:
                # Convert each line into a list of integers, skip empty lines
                if line.strip():  # Check if the line is not empty
                    row = [int(float(val) / 10) for val in line.split()]
                    matrix.append(row)
            elif "EDGE_WEIGHT_SECTION" in line:
                start_reading = True
    return matrix


# Parse the matrix and compress it
matrix = parse_matrix("C:/Users/Uporabnik/OneDrive/Desktop/VečpredstavnostProjekt/duration_matrix.txt")
compressed_matrix = compress_matrix(matrix)

 # Optionally, write the compressed matrix to a file
writeToFile(compressed_matrix, "../assets/compressed_time_matrix.bin")
