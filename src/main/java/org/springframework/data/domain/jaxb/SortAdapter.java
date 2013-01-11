package org.springframework.data.domain.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.jaxb.SpringDataJaxb.SortDto;

/**
 * {@link XmlAdapter} to convert {@link Sort} instances into {@link SortDto} instances and vice versa.
 * 
 * @author Oliver Gierke
 */
public class SortAdapter extends XmlAdapter<SortDto, Sort> {

	public static final SortAdapter INSTANCE = new SortAdapter();

	/*
	 * (non-Javadoc)
	 * @see javax.xml.bind.annotation.adapters.XmlAdapter#marshal(java.lang.Object)
	 */
	@Override
	public SortDto marshal(Sort source) throws Exception {

		if (source == null) {
			return null;
		}

		SortDto dto = new SortDto();
		dto.orders = SpringDataJaxb.marshal(source, OrderAdapter.INSTANCE);

		return dto;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.xml.bind.annotation.adapters.XmlAdapter#unmarshal(java.lang.Object)
	 */
	@Override
	public Sort unmarshal(SortDto source) throws Exception {
		return source == null ? null : new Sort(SpringDataJaxb.unmarshal(source.orders, OrderAdapter.INSTANCE));
	}
}
