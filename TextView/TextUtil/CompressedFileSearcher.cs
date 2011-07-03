using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace TextUtil
{
    public class CompressedFileSearcher
    {
        private class SpanSearchState
        {
            public SpanSearchState(CompressedStorage.Span s)
            {
                Span = s;
            }

            public CompressedStorage.Span Span { get; private set; }

            public bool IsReady { get; private set; }

            public List<MatchResult> Matches { get; private set; }

            // PRE  : buffer can be null, is there are no results
            public void AddResult(MatchResult result)
            {
                Debug.Assert(!IsReady);

                if (Matches == null)
                    Matches = new List<MatchResult>();

                Matches.Add(result);
            }

            public void OnDone()
            {
                IsReady = true;
            }
        }

        public struct MatchResult
        {
            public string Line { get; private set; }

            public string LineNumber { get; private set; }

            public MatchResult(string line, long lineNumber) : this()
            {
                Line = line;
                LineNumber = lineNumber.ToString("N0");
            }

            public static MatchResult SearchTerminated
            {
                get { return new MatchResult {LineNumber = "N/A", Line = "...  Search Terminated  ..."}; }
            }
        }

        private bool m_continue;
        private CompressedStorage m_contents;
        private string m_fileId;
        private SpanSearchState[] m_spanMatches;
        private int m_takenSpans = 0;

        private int m_maxMatches = 1000;

        public CompressedFileSearcher(CompressedStorage contents, string fileId)
        {
            m_contents = contents;
            m_fileId = fileId;
        }

        public void StartFind(string toFind, int maxMatches)
        {
            m_continue = true;

            var spans = m_contents.GetCurrentSpans(m_fileId);
            m_takenSpans = 0;
            m_spanMatches = spans.Select(s => new SpanSearchState(s)).ToArray();

            Task.Factory.StartNew(() => m_spanMatches.AsParallel().ForAll(s => SearchSpan(s, toFind)));
        }

        public bool GetNewResults(ICollection<MatchResult> results, out int findProgressPercent)
        {
            if (m_spanMatches == null)
            {
                findProgressPercent = 0;
                return true;
            }

            findProgressPercent = 100 * m_takenSpans / m_spanMatches.Length;
            while (m_takenSpans < m_spanMatches.Length)
            {
                var nextResult = m_spanMatches[m_takenSpans];
                if (!nextResult.IsReady)
                    return false;

                if (nextResult.Matches != null)
                {
                    foreach (var matchResult in nextResult.Matches)
                    {
                        results.Add(matchResult);
                        if (results.Count >= m_maxMatches)
                        {
                            results.Add(MatchResult.SearchTerminated);
                            StopFind();
                            findProgressPercent = 100;
                            return true;
                        }
                    }
                }

                m_takenSpans++;
                findProgressPercent = 100 * m_takenSpans / m_spanMatches.Length;
            }

            return true;
        }

        public void StopFind()
        {
            m_continue = false;

            m_takenSpans = 0;
            m_spanMatches = null;
        }

        private void SearchSpan(SpanSearchState spanState, string findStr)
        {
            var span = spanState.Span;

            long lineNumber = span.FirstLine;

            if (m_continue)
            {
                // Once we start searching a span, we will finish it off
                // However, don't start if we are already full of matches, or been cancelled
                foreach (var line in span.GetContents())
                {
                    bool lineMatches = line.IndexOf(findStr, StringComparison.CurrentCultureIgnoreCase) >= 0;
                    if (lineMatches)
                        spanState.AddResult(new MatchResult(line, lineNumber));

                    lineNumber++;
                }
            }

            spanState.OnDone();
        }
    }
}
