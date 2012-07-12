XML/SOAP Ersatz tools, for Scala to read/write XML/SOAP without code generation, annotations or whatever magic. 

These tools are developed & used with Play Framework 2.0 Scala.

You may use this api if like me:
- you don't like SOAP because it's not human-friendly at all, it's too verbose and the surrounding standards are just non-sense
- you hate WSDL because it's not humanly readable and requires tools to manipulate them
- you must pollute your code with esoteric annotations just to do RPC in the great majority of cases 
- you hate all those bloated frameworks to manage SOAP and the crazy standards around it
- you don't understand why we still use SOAP but you still have to communicate in SOAP in many cases so you still must bear it
- you just want to extract the data you need from SOAP frames you receive and to generate SOAP frames that look like what your client expect but you don't care about the standards behind that.

It DOES NOT pretend to provide pure standard SOAP.
It DOES NOT generate WSDL so you must provide it yourself.
It just helps you mimic SOAP by providing a few tools and helpers to deserialize/serialize.
It just aims at being practical without needing deep knownledge of SOAP standards.
It can serialize/deserialize SOAP so it can also do it for XML...
It uses pure Scala XML library even if it's a bit incoherent sometimes. AntiXML could be cool too...

This is a draft module providing raw mechanisms. Don't hesitate to contribute to make it better ;)

Have fun!
Pascal

