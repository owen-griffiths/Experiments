using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using ICSharpCode.SharpZipLib.Zip.Compression.Streams;
using ICSharpCode.SharpZipLib.Zip.Compression;
using System.Runtime.Serialization.Formatters.Binary;
using System.Diagnostics;

namespace TextUtil
{
    public class CompressUtil
    {
        public static T Uncompress<T>(byte[] compressed)
        {
            var memStream = new MemoryStream(compressed);
            using (var inflater = new InflaterInputStream(memStream, new Inflater(true)))
            {
                BinaryFormatter formatter = new BinaryFormatter();
                return (T)formatter.Deserialize(inflater);
            }
        }

        public static byte[] Compress(object data)
        {
            MemoryStream memStream = new MemoryStream();
            BinaryFormatter formatter = new BinaryFormatter();

            using (var deflater = new DeflaterOutputStream(memStream, new Deflater(5, true)))
            {
                formatter.Serialize(deflater, data);
            }

            return memStream.ToArray();
        }
    }
}
