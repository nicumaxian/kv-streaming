package com.koniosoftworks.kvstreaming.data.io.algorithms;

import com.koniosoftworks.kvstreaming.domain.dto.messages.InitializationMessage;
import com.koniosoftworks.kvstreaming.domain.io.StreamReader;
import com.koniosoftworks.kvstreaming.domain.io.SerializationAlgorithm;
import com.koniosoftworks.kvstreaming.domain.io.StreamWriter;

/**
 * Created by Nicu Maxian on 5/19/2017.
 */
public class InitializationMessageSerializationAlgorithm implements SerializationAlgorithm<InitializationMessage> {
    @Override
    public void serialize(StreamWriter streamWriter, InitializationMessage object) {
        streamWriter.put(object.getUdpPort());
        streamWriter.put(object.getUsername());
    }

    @Override
    public void deserialize(StreamReader streamReader, InitializationMessage object) {
        object.setUdpPort(streamReader.nextInt());
        object.setUsername(streamReader.nextString());
    }
}
