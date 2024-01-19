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

def decompress(compressed):
    arr = []
    arr.append(int(compressed[0:8],2))
    i = 8

    while i < len(compressed):
        simbol = compressed[i]
        simbol += compressed[i+1]
        if simbol == '00': #razlike
            secondsimbol = compressed[i+2]
            secondsimbol += compressed[i+3]
            if secondsimbol == '00':          
                inv_mapping = {v: k for k, v in mapping.items()}
                thirdsimbol = compressed[i+4]
                thirdsimbol +=compressed[i+5]
                arr.append(inv_mapping[thirdsimbol])
                i+=6
            elif secondsimbol == '01':
                inv_mapping = {v: k for k, v in mapping.items()}
                thirdsimbol = compressed[i+4]
                thirdsimbol +=compressed[i+5]
                thirdsimbol +=compressed[i+6]
                arr.append(inv_mapping[thirdsimbol])
                i+=7
            elif secondsimbol == '10':
                inv_mapping = {v: k for k, v in mapping.items()}
                thirdsimbol = compressed[i+4]
                thirdsimbol +=compressed[i+5]
                thirdsimbol +=compressed[i+6]
                thirdsimbol +=compressed[i+7]
                arr.append(inv_mapping[thirdsimbol])
                i+=8
            elif secondsimbol == '11':
                inv_mapping = {v: k for k, v in mapping.items()}
                thirdsimbol = compressed[i+4]
                thirdsimbol +=compressed[i+5]
                thirdsimbol +=compressed[i+6]
                thirdsimbol +=compressed[i+7]
                thirdsimbol +=compressed[i+8]
                arr.append(inv_mapping[thirdsimbol])
                i+=9
        if simbol == '01': #ponovitve
            binary_part = compressed[i+2:i+5]
            count = int(binary_part, 2)+1
            j=0
            while j < count:
                arr.append(0)
                j+=1

            i+=5
        if simbol == '10': #absolutno kodiranje
            binary_part=compressed[i+3:i+11]
            if compressed[i+2] == '0':
                arr.append(int(binary_part,2))
            elif compressed[i+2] == '1':
                arr.append(-int(binary_part,2))
            i+=11
        if simbol == '11': #konec
            return arr


def convertToNumbers(decompressedArr):
    r = []
    r.append(decompressedArr[0])
    i=1
    while i < len(decompressedArr):
        if(decompressedArr[i] == 0):
            r.append(r[i-1])
            i+=1
        else:     
            sum = r[i-1] + decompressedArr[i]
            r.append(sum)
            i+=1
    return r


def save_matrix_to_file(matrix, file_path):
    header = """NAME: distance_matrix
TYPE: TSP
DIMENSION: 10
EDGE_WEIGHT_TYPE: EXPLICIT
EDGE_WEIGHT_FORMAT: FULL_MATRIX
DISPLAY_DATA_TYPE: TWOD_DISPLAY
EDGE_WEIGHT_SECTION
"""
    with open(file_path, 'w') as file:
        file.write(header)  # Write the header text first
        for row in matrix:
            row_string = ' '.join(map(str, row))
            file.write(row_string + '\n')  # Then write each row of the matrix


def openFile(file_path):
    with open(file_path, 'rb') as file:
        byte_value_from_file = file.read()
    string_from_file = byte_value_from_file.decode('utf-8')
    return string_from_file

def decompress_matrix(compressed_matrix):
    # Each row is separated by a newline character
    rows = compressed_matrix.split("\n")  
    matrix = []
    for row in rows:
        if row.strip():  # Check if the row is not empty
            decompressed_row = decompress(row)
            numbers_row = convertToNumbers(decompressed_row)
            matrix.append(numbers_row)
    return matrix


def execute(inPath, outPath):
    compressed_matrix_data = openFile(inPath)
    matrix = decompress_matrix(compressed_matrix_data)
    save_matrix_to_file(matrix, outPath)


