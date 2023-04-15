import React from 'react';
import { useNavigate } from '@tramvai/module-router';
import { Box, Flex, Title, Button, Space } from '@mantine/core';

interface Props {
  title: string;
  backText?: string;
  backPath?: string;
  right?: React.ReactNode;
}

export default function PageHeader(props: Props) {
  const { title, backText, backPath = '', right } = props;
  const navigate = useNavigate(backPath);
  const backButton = backText && backPath && (
    <Button variant="subtle" compact color="gray" onClick={navigate} p={0}>
      {backText}
    </Button>
  );
  return (
    <Box mt="xl" mb="lg">
      {backButton || <Space h="1.625rem" />}
      <Flex direction="row" justify="space-between" align="center">
        <Title order={3}>{title}</Title>
        <Box>{right}</Box>
      </Flex>
    </Box>
  );
}
