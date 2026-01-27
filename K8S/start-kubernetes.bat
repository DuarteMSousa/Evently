@echo off

kubectl apply -f ./infrastructure --recursive
kubectl apply -f ./ingress

kubectl apply -f ./servicesWithoutEureka/cart

kubectl apply -f ./servicesWithoutEureka/events

kubectl apply -f ./servicesWithoutEureka/fileGeneration

timeout /t 60

kubectl apply -f ./servicesWithoutEureka/notifications

kubectl apply -f ./servicesWithoutEureka/orders

kubectl apply -f ./servicesWithoutEureka/organizations

timeout /t 60

kubectl apply -f ./servicesWithoutEureka/payments

kubectl apply -f ./servicesWithoutEureka/refunds

kubectl apply -f ./servicesWithoutEureka/reviews

timeout /t 60

kubectl apply -f ./servicesWithoutEureka/ticketManagement

kubectl apply -f ./servicesWithoutEureka/tickets

timeout /t 60

kubectl apply -f ./servicesWithoutEureka/users

kubectl apply -f ./servicesWithoutEureka/venues


pause
