package example;

import java.util.Collection;
import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

public interface SampleDto {

	String getName();

	@DateTimeFormat(iso = ISO.DATE)
	Date getDate();

	SampleDto.Address getBillingAddress();

	Collection<SampleDto.Address> getShippingAddresses();

	public interface Address {

		String getZipCode();

		String getCity();
	}
}
