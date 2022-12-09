# search_engine

This application is a search engine based on the document collection provided by TREC-2020, containing 8.8M documents, of which the totality was indexed and made ready to perform queries on. The application allows a user to:

<ul><li>Extract the document collection if not already extracted.</li><br>
<li>Build the InvertedIndex, DocumentIndex, Lecicon, InvertedIndexFrequency, and MetaDataCollection files, which are all needed for using the search engine, if not already generated. This step includes the following choices:</li>

<ul>
<li> Choose how many documents to Index, if not the entire collection.</li>
<li> Choose whether to use stemming or not.</li>
<li> Choose whether to remove stopwords or not.</li>
</ul><br>

<li>Submit single, conjunctive or disjunctive queries and:</li>

<ul>
<li> Choose the scoring method: TF-IDF or BM25 Okapi. </li>
<li> Retrieve <em>k</em> most revelant documents for the query. </li>
</ul>
