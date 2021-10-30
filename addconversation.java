
  public static InputStream asInputStream(Reader , Charset , int bufferSize,
                                          CodingErrorAction newAction) {
    return new ReaderInputStream(reader,
            charset.newEncoder()
                    .onMalformedInput(newAction)
                     .onUnmappableCharacter(newAction),
            bufferSize);
  }