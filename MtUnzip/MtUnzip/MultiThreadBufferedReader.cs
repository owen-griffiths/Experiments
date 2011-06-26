using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;

namespace MtUnzip
{
    class MultiThreadBufferedReader : Stream
    {
        public const int BufferSize = 1024 * 1024;
        public const int MaxBuffers = 2;

        public MultiThreadBufferedReader(Stream source)
        {
            for (int i = 0; i < MaxBuffers; i++)
            {
                m_emptyBuffers.Add(new byte[BufferSize]);
            }
            m_usedBuffersEnum = m_usedBuffers.GetConsumingEnumerable().GetEnumerator();

            m_sourceReader = source;

            var rdThread = new Thread(ReadSource);
            rdThread.Start();
        }

        protected override void Dispose(bool disposing)
        {
            m_sourceReader.Dispose();
            base.Dispose(disposing);
        }

        public override bool CanRead
        {
            get { return true; }
        }

        public override bool CanSeek
        {
            get { return false; }
        }

        public override bool CanWrite
        {
            get { return false; }
        }

        public override void Flush()
        {
        }

        public override long Length
        {
            get { throw new NotSupportedException(); }
        }

        public override long Position
        {
            get
            {
                throw new NotSupportedException();
            }
            set
            {
                throw new NotSupportedException();
            }
        }

        public override int Read(byte[] outBuffer, int outOffset, int count)
        {
            if (m_currentSource.Data == null)
            {
                // Try to get the next piece of source data.  This will block until the read thread either
                //  fills another buffer, or indicates that it has finished reading
                if (!m_usedBuffersEnum.MoveNext())
                {
                    return 0;
                }

                m_currentSource = m_usedBuffersEnum.Current;
                m_sourceOffset = 0;
            }

            Debug.Assert(m_sourceOffset < m_currentSource.Length);

            int result = Math.Min(count, m_currentSource.Length - m_sourceOffset);

            Array.Copy(m_currentSource.Data, m_sourceOffset, outBuffer, outOffset, result);
            m_sourceOffset += result;

            Debug.Assert(m_sourceOffset <= m_currentSource.Length);

            if (m_sourceOffset >= m_currentSource.Length)
            {
                // Exhausted current buffer - return it to the pool
                m_emptyBuffers.Add(m_currentSource.Data);
                m_currentSource = new Buffer();
            }

            return result;
        }

        public override long Seek(long offset, SeekOrigin origin)
        {
            throw new NotSupportedException();
        }

        public override void SetLength(long value)
        {
            throw new NotSupportedException();
        }

        public override void Write(byte[] buffer, int offset, int count)
        {
            throw new NotSupportedException();
        }


        private struct Buffer
        {
            public byte[] Data { get; private set; }
            public int Length { get; private set; }

            public Buffer(byte[] data, int length)
                : this()
            {
                Debug.Assert(length > 0);
                Debug.Assert(length <= data.Length);

                Data = data;
                Length = length;
            }
        }

        private int ReadBlock(byte[] buffer, Stream s)
        {
            int currentBlockSize;
            int totalRead = 0;

            do
            {
                currentBlockSize = s.Read(buffer, totalRead, buffer.Length - totalRead);
                totalRead += currentBlockSize;
            }
            while ((currentBlockSize > 0) && (totalRead < buffer.Length));

            return totalRead;
        }


        private void ReadSource()
        {
            // This foreach should actually never terminate, as the processing side will keep recycling the buffers
            // The only way out is the return when end of stream detected
            foreach (var buffer in m_emptyBuffers.GetConsumingEnumerable())
            {
                var countRead = ReadBlock(buffer, m_sourceReader);

                if (countRead > 0)
                {
                    m_usedBuffers.Add(new Buffer(buffer, countRead));
                }

                if (countRead < buffer.Length)
                {
                    m_usedBuffers.CompleteAdding();
                    return;
                }
            }

            Debug.Assert(false, "Run out of buffers");
        }

        private BlockingCollection<byte[]> m_emptyBuffers = new BlockingCollection<byte[]>();
        private BlockingCollection<Buffer> m_usedBuffers = new BlockingCollection<Buffer>();
        private IEnumerator<Buffer> m_usedBuffersEnum;

        private Stream m_sourceReader;

        private Buffer m_currentSource;
        private int m_sourceOffset;
    }
}
