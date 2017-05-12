package ch.datascience.graph.elements.persistence.impl

import ch.datascience.graph.elements.persistence.{PersistedRichRecordProperty, RecordPath}
import ch.datascience.graph.elements.{BoxedOrValidValue, Properties, RichProperty}

/**
  * Created by johann on 11/05/17.
  */
case class ImplPersistedRecordRichProperty[+Key, +Value: BoxedOrValidValue, MetaKey, +MetaValue: BoxedOrValidValue](
  parent: RecordPath,
  key: Key,
  value: Value,
  properties: Properties[MetaKey, MetaValue, ImplPersistedRecordProperty[MetaKey, MetaValue]]
) extends PersistedRichRecordProperty[Key, Value, ImplPersistedRecordRichProperty[Key, Value, MetaKey, MetaValue]]
  with RichProperty[Key, Value, MetaKey, MetaValue, ImplPersistedRecordProperty[MetaKey, MetaValue], ImplPersistedRecordRichProperty[Key, Value, MetaKey, MetaValue]]
