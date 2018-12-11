/**
 *
 */
package de.hybris.support.facades.impl;

import de.hybris.platform.commercefacades.customer.impl.DefaultCustomerFacade;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.order.exceptions.CalculationException;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.util.Config;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;


/**
 * @author support demo
 *
 */
public class OptCustomerFacade extends DefaultCustomerFacade
{
	private static final Logger LOG = Logger.getLogger(OptCustomerFacade.class);

	private static final String SELECT_PK_FROM_CART_WHERE_PK_PK = "SELECT {pk} FROM {Cart} WHERE {pk} != ?pk";

	private static final String READ_CARTS_FROM_DB = "read.carts.from.db";
	private static final String RECALCULATE_CART = "recalculate.cart";

	private static final AtomicInteger nextId = new AtomicInteger(0);

	// Thread local variable containing each thread's ID
	private static final ThreadLocal cartId = new ThreadLocal()
	{
		@Override
		protected Integer initialValue()
		{
			return Integer.valueOf(nextId.getAndIncrement());
		}
	};

	private static List<CartModel> cartList = new CopyOnWriteArrayList<>();

	private FlexibleSearchService flexibleSearchService;

	/*
	 * (non-Javadoc)
	 *
	 * @see de.hybris.platform.commercefacades.customer.impl.DefaultCustomerFacade#loginSuccess()
	 */
	@Override
	public void loginSuccess()
	{
		LOG.debug(">>>>>>>>>>>OptCustomerFacade<<<<<<<<<<<<<");
		super.loginSuccess();
		checkCartList();
	}

	/**
	 *
	 */
	private void checkCartList()
	{
		final boolean readCartsFromDB = Config.getBoolean(READ_CARTS_FROM_DB, false);

		if (readCartsFromDB)
		{
			readListOfCartsfs();
			recalculateCarts();
		}
		else
		{
			readListOfCarts();
		}
	}

	/**
	 * Recalculate carts
	 */
	private void recalculateCarts()
	{
		final boolean recalculateCarts = Config.getBoolean(RECALCULATE_CART, false);
		if (recalculateCarts)
		{
			for (final CartModel cartModel : getListOfCarts())
			{
				try
				{
					getCommerceCartService().recalculateCart(cartModel);
				}
				catch (final CalculationException e)
				{
					e.printStackTrace();
				}
				getModelService().save(cartModel);
			}
		}
	}

	/**
	 *
	 */
	private void readListOfCarts()
	{
		final boolean readCartsFromDB = Config.getBoolean(READ_CARTS_FROM_DB, false);

		final CartModel cartModel = createCartModel();
		while (!readCartsFromDB)
		{
			final CartModel itemCart = getModelService().clone(cartModel);
			itemCart.setCode(getCode());
			addToCartList(itemCart);
		}
	}

	/**
	 * @return CartModel
	 */
	private CartModel createCartModel()
	{
		final CartModel ret = new CartModel();
		ret.setCode(getCode());
		ret.setName(ret.getCode());
		return ret;
	}

	/**
	 * @return String Code
	 */
	private String getCode()
	{
		return UUID.randomUUID().toString();
	}

	private List<CartModel> getListOfCarts()
	{
		if (cartList != null)
		{
			LOG.info(" size of concurrent list : " + cartList.size());
		}
		return cartList;
	}

	private void readListOfCartsfs()
	{

		final FlexibleSearchQuery query = new FlexibleSearchQuery(SELECT_PK_FROM_CART_WHERE_PK_PK);
		query.addQueryParameter("pk", "8098888880");

		final SearchResult<CartModel> result = flexibleSearchService.search(query);
		final List<CartModel> listOfCarts = result.getResult();

		if (listOfCarts != null)
		{
			LOG.info(" listOfCarts : " + listOfCarts.size());
		}

		addToCartList(listOfCarts);
	}


	private void addToCartList(final CartModel cart)
	{
		if (cartList == null)
		{
			cartList = new CopyOnWriteArrayList<CartModel>();
		}

		cartList.add(cart);
	}

	private void addToCartList(final List<CartModel> listOfCarts)
	{
		if (cartList == null)
		{
			cartList = listOfCarts;
		}
		else
		{
			cartList.addAll(listOfCarts);
		}
	}

	@Required
	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearch)
	{
		this.flexibleSearchService = flexibleSearch;
	}

	protected FlexibleSearchService getFlexibleSearchService()
	{
		return flexibleSearchService;
	}

}