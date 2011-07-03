using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.Linq;
using System.Text;

namespace WpfTextView
{
    class ListEnumerator : IEnumerator
    {
        public ListEnumerator(IList src)
        {
            m_data = src;
            m_count = src.Count;
            m_cursor = -1;
        }
        public object Current
        {
            get { return m_data[m_cursor]; }
        }

        public void Dispose()
        {
        }

        public bool MoveNext()
        {
            m_cursor++;
            return m_cursor < m_count;
        }

        public void Reset()
        {
            m_cursor = -1;
        }

        private readonly int m_count;
        private int m_cursor;
        private readonly IList m_data;
    }

    class HardCodeList : IList, INotifyCollectionChanged
    {
        public object this[int index]
        {
            get
            {
                s_accessCount++; 
                return index.ToString("N0"); 
            }
            set
            {
                throw new NotImplementedException();
            }
        }

        public void Clear()
        {
            throw new NotImplementedException();
        }

        public int Count
        {
            get { return 1000 * 1000; }
        }

        private static int s_accessCount;


        public int Add(object value)
        {
            throw new NotImplementedException();
        }

        public bool Contains(object value)
        {
            return false;
        }

        public int IndexOf(object value)
        {
            return 0;
        }

        public void Insert(int index, object value)
        {
            throw new NotImplementedException();
        }

        public bool IsFixedSize
        {
            get { throw new NotImplementedException(); }
        }

        public void Remove(object value)
        {
            throw new NotImplementedException();
        }

        public void CopyTo(Array array, int index)
        {
            throw new NotImplementedException();
        }

        public bool IsSynchronized
        {
            get { return false; }
        }

        public object SyncRoot
        {
            get
            {
                return this; 
            }
        }

        public bool IsReadOnly
        {
            get { throw new NotImplementedException(); }
        }

        public void RemoveAt(int index)
        {
            throw new NotImplementedException();
        }

        public IEnumerator GetEnumerator()
        {
            return new ListEnumerator(this);
        }

        public void OnUpdate()
        {
            if (CollectionChanged != null)
            {
                CollectionChanged(this, new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Reset));
            }
        }

        public event NotifyCollectionChangedEventHandler CollectionChanged;

    }
}
