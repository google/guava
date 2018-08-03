package com.google.common.base;

import com.google.common.annotations.GwtCompatible;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@GwtCompatible(emulated = true)
public class LazyReferenceTest
{
    @Test
    public void goLazy()
            throws IOException
    {
        final LazyReference<Connection> connection = LazyReference.goLazy(() -> {
            try {
                return DriverManager.getConnection("jdbc:url....");
            }
            catch (SQLException e) {
                throw new RuntimeException("Connection create fail", e);
            }
        });

        Assert.assertNotNull(serialize(connection));
    }

    private static byte[] serialize(Serializable serializable)
            throws IOException
    {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream os = new ObjectOutputStream(bos)
        ) {
            os.writeObject(serializable);
            return bos.toByteArray();
        }
    }
}
